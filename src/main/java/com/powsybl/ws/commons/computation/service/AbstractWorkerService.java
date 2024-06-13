/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france.com>
 * @param <S> powsybl Result class specific to the computation
 * @param <R> Run context specific to a computation, including parameters
 * @param <P> powsybl and gridsuite Parameters specifics to the computation
 * @param <T> result service specific to the computation
 */
public abstract class AbstractWorkerService<S, R extends AbstractComputationRunContext<P>, P, T extends AbstractComputationResultService<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorkerService.class);

    protected final Lock lockRunAndCancel = new ReentrantLock();
    protected final ObjectMapper objectMapper;
    protected final NetworkStoreService networkStoreService;
    protected final ReportService reportService;
    protected final ExecutionService executionService;
    protected final NotificationService notificationService;
    protected final AbstractComputationObserver<S, P> observer;
    protected final Map<UUID, CompletableFuture<S>> futures = new ConcurrentHashMap<>();
    protected final Map<UUID, CancelContext> cancelComputationRequests = new ConcurrentHashMap<>();
    protected final T resultService;

    protected AbstractWorkerService(NetworkStoreService networkStoreService,
                                    NotificationService notificationService,
                                    ReportService reportService,
                                    T resultService,
                                    ExecutionService executionService,
                                    AbstractComputationObserver<S, P> observer,
                                    ObjectMapper objectMapper) {
        this.networkStoreService = networkStoreService;
        this.notificationService = notificationService;
        this.reportService = reportService;
        this.resultService = resultService;
        this.executionService = executionService;
        this.observer = observer;
        this.objectMapper = objectMapper;
    }

    protected PreloadingStrategy getNetworkPreloadingStrategy() {
        return PreloadingStrategy.COLLECTION;
    }

    protected Network getNetwork(UUID networkUuid, String variantId) {
        try {
            Network network = networkStoreService.getNetwork(networkUuid, getNetworkPreloadingStrategy());
            String variant = StringUtils.isBlank(variantId) ? VariantManagerConstants.INITIAL_VARIANT_ID : variantId;
            network.getVariantManager().setWorkingVariant(variant);
            return network;
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    protected void cleanResultsAndPublishCancel(UUID resultUuid, String receiver) {
        resultService.delete(resultUuid);
        notificationService.publishStop(resultUuid, receiver, getComputationType());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} (resultUuid='{}')",
                    NotificationService.getCancelMessage(getComputationType()),
                    resultUuid);
        }
    }

    private void cancelAsync(CancelContext cancelContext) {
        lockRunAndCancel.lock();
        try {
            cancelComputationRequests.put(cancelContext.resultUuid(), cancelContext);

            // find the completableFuture associated with result uuid
            CompletableFuture<S> future = futures.get(cancelContext.resultUuid());
            if (future != null) {
                future.cancel(true);  // cancel computation in progress
            }
            cleanResultsAndPublishCancel(cancelContext.resultUuid(), cancelContext.receiver());
        } finally {
            lockRunAndCancel.unlock();
        }
    }

    protected abstract AbstractResultContext<R> fromMessage(Message<String> message);

    protected boolean resultCanBeSaved(S result) {
        return result != null;
    }

    public Consumer<Message<String>> consumeRun() {
        return message -> {
            AbstractResultContext<R> resultContext = fromMessage(message);
            try {
                long startTime = System.nanoTime();

                Network network = getNetwork(resultContext.getRunContext().getNetworkUuid(),
                        resultContext.getRunContext().getVariantId());
                resultContext.getRunContext().setNetwork(network);
                S result = run(resultContext.getRunContext(), resultContext.getResultUuid());

                LOGGER.info("Just run in {}s", TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime));

                if (resultCanBeSaved(result)) {
                    startTime = System.nanoTime();
                    observer.observe("results.save", resultContext.getRunContext(), () -> saveResult(network, resultContext, result));

                    LOGGER.info("Stored in {}s", TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime));

                    sendResultMessage(resultContext, result);
                    LOGGER.info("{} complete (resultUuid='{}')", getComputationType(), resultContext.getResultUuid());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                if (!(e instanceof CancellationException)) {
                    LOGGER.error(NotificationService.getFailedMessage(getComputationType()), e);
                    publishFail(resultContext, e.getMessage());
                    resultService.delete(resultContext.getResultUuid());
                    this.handleNonCancellationException(resultContext, e);
                }
            } finally {
                futures.remove(resultContext.getResultUuid());
                cancelComputationRequests.remove(resultContext.getResultUuid());
            }
        };
    }

    /**
     * Handle exception in consumeRun that is not a CancellationException
     * @param resultContext The context of the computation
     * @param exception The exception to handle
     */
    protected void handleNonCancellationException(AbstractResultContext<R> resultContext, Exception exception) {
    }

    public Consumer<Message<String>> consumeCancel() {
        return message -> cancelAsync(CancelContext.fromMessage(message));
    }

    protected abstract void saveResult(Network network, AbstractResultContext<R> resultContext, S result);

    protected void sendResultMessage(AbstractResultContext<R> resultContext, S ignoredResult) {
        notificationService.sendResultMessage(resultContext.getResultUuid(), resultContext.getRunContext().getReceiver(),
                resultContext.getRunContext().getUserId(), null);
    }

    protected void publishFail(AbstractResultContext<R> resultContext, String message) {
        notificationService.publishFail(resultContext.getResultUuid(), resultContext.getRunContext().getReceiver(),
                message, resultContext.getRunContext().getUserId(), getComputationType(), null);
    }

    /**
     * Do some extra task before running the computation, e.g. print log or init extra data for the run context
     * @param ignoredRunContext This context may be used for further computation in overriding classes
     */
    protected void preRun(R ignoredRunContext) {
        LOGGER.info("Run {} computation...", getComputationType());
    }

    protected S run(R runContext, UUID resultUuid) throws Exception {
        String provider = runContext.getProvider();
        AtomicReference<ReportNode> rootReporter = new AtomicReference<>(ReportNode.NO_OP);
        ReportNode reportNode = ReportNode.NO_OP;

        if (runContext.getReportInfos() != null && runContext.getReportInfos().reportUuid() != null) {
            final String reportType = runContext.getReportInfos().computationType();
            String rootReporterId = runContext.getReportInfos().reporterId() == null ? reportType : runContext.getReportInfos().reporterId() + "@" + reportType;
            rootReporter.set(ReportNode.newRootReportNode().withMessageTemplate(rootReporterId, rootReporterId).build());
            reportNode = rootReporter.get().newReportNode().withMessageTemplate(reportType, reportType + (provider != null ? " (" + provider + ")" : ""))
                    .withUntypedValue("providerToUse", Objects.requireNonNullElse(provider, "")).add();
            // Delete any previous computation logs
            observer.observe("report.delete",
                    runContext, () -> reportService.deleteReport(runContext.getReportInfos().reportUuid(), reportType));
        }
        runContext.setReportNode(reportNode);

        preRun(runContext);
        CompletableFuture<S> future = runAsync(runContext, provider, resultUuid);
        S result = future == null ? null : observer.observeRun("run", runContext, future::get);
        postRun(runContext, rootReporter, result);
        return result;
    }

    /**
     * Do some extra task after running the computation
     * @param runContext This context may be used for extra task in overriding classes
     * @param rootReportNode root of the reporter tree
     * @param ignoredResult The result of the computation
     */
    protected void postRun(R runContext, AtomicReference<ReportNode> rootReportNode, S ignoredResult) {
        if (runContext.getReportInfos().reportUuid() != null) {
            observer.observe("report.send", runContext, () -> reportService.sendReport(runContext.getReportInfos().reportUuid(), rootReportNode.get()));
        }
    }

    protected CompletableFuture<S> runAsync(
            R runContext,
            String provider,
            UUID resultUuid) {
        lockRunAndCancel.lock();
        try {
            if (resultUuid != null && cancelComputationRequests.get(resultUuid) != null) {
                return null;
            }
            CompletableFuture<S> future = getCompletableFuture(runContext, provider, resultUuid);
            if (resultUuid != null) {
                futures.put(resultUuid, future);
            }
            return future;
        } finally {
            lockRunAndCancel.unlock();
        }
    }

    protected abstract String getComputationType();

    protected abstract CompletableFuture<S> getCompletableFuture(R runContext, String provider, UUID resultUuid);
}
