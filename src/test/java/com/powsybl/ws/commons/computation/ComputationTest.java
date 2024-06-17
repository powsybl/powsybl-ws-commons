package com.powsybl.ws.commons.computation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.ws.commons.computation.dto.ReportInfos;
import com.powsybl.ws.commons.computation.service.AbstractComputationObserver;
import com.powsybl.ws.commons.computation.service.AbstractComputationResultService;
import com.powsybl.ws.commons.computation.service.AbstractComputationRunContext;
import com.powsybl.ws.commons.computation.service.AbstractComputationService;
import com.powsybl.ws.commons.computation.service.AbstractResultContext;
import com.powsybl.ws.commons.computation.service.AbstractWorkerService;
import com.powsybl.ws.commons.computation.service.ExecutionService;
import com.powsybl.ws.commons.computation.service.NotificationService;
import com.powsybl.ws.commons.computation.service.ReportService;
import com.powsybl.ws.commons.computation.service.UuidGeneratorService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.powsybl.ws.commons.computation.ComputationTest.MockComputationStatus.COMPLETED;
import static com.powsybl.ws.commons.computation.ComputationTest.MockComputationStatus.NOT_DONE;
import static com.powsybl.ws.commons.computation.service.NotificationService.HEADER_RECEIVER;
import static com.powsybl.ws.commons.computation.service.NotificationService.HEADER_RESULT_UUID;
import static com.powsybl.ws.commons.computation.service.NotificationService.HEADER_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({ MockitoExtension.class })
@Slf4j
class ComputationTest implements WithAssertions {
    private static final String COMPUTATION_TYPE = "mockComputation";
    @Mock
    private NetworkStoreService networkStoreService;
    @Mock
    private ReportService reportService;
    @Mock
    private ExecutionService executionService;
    @Mock
    private UuidGeneratorService uuidGeneratorService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Network network;

    public static class MockComputationResult { }

    public static class MockComputationParameters { }

    public enum MockComputationStatus {
        NOT_DONE,
        RUNNING,
        COMPLETED
    }

    public static class MockComputationResultService extends AbstractComputationResultService<MockComputationStatus> {
        Map<UUID, MockComputationStatus> mockDBStatus = new HashMap<>();

        @Override
        public void insertStatus(List<UUID> resultUuids, MockComputationStatus status) {
            resultUuids.forEach(uuid ->
                    mockDBStatus.put(uuid, status));
        }

        @Override
        public void delete(UUID resultUuid) {
            mockDBStatus.remove(resultUuid);
        }

        @Override
        public void deleteAll() {
            mockDBStatus.clear();
        }

        @Override
        public MockComputationStatus findStatus(UUID resultUuid) {
            return mockDBStatus.get(resultUuid);
        }
    }

    public static class MockComputationObserver extends AbstractComputationObserver<MockComputationResult, MockComputationParameters> {
        protected MockComputationObserver(@NonNull ObservationRegistry observationRegistry, @NonNull MeterRegistry meterRegistry) {
            super(observationRegistry, meterRegistry);
        }

        @Override
        protected String getComputationType() {
            return COMPUTATION_TYPE;
        }

        @Override
        protected String getResultStatus(MockComputationResult res) {
            return res != null ? "OK" : "NOK";
        }
    }

    public static class MockComputationRunContext extends AbstractComputationRunContext<MockComputationParameters> {
        // makes the mock computation to behave in a specific way
        @Getter @Setter
        ComputationResultWanted computationResWanted = ComputationResultWanted.SUCCESS;

        protected MockComputationRunContext(UUID networkUuid, String variantId, String receiver, ReportInfos reportInfos,
                                            String userId, String provider, MockComputationParameters parameters) {
            super(networkUuid, variantId, receiver, reportInfos, userId, provider, parameters);
        }
    }

    public static class MockComputationResultContext extends AbstractResultContext<MockComputationRunContext> {
        protected MockComputationResultContext(UUID resultUuid, MockComputationRunContext runContext) {
            super(resultUuid, runContext);
        }
    }

    public class MockComputationService extends AbstractComputationService<MockComputationRunContext, MockComputationResultService, MockComputationStatus> {
        protected MockComputationService(NotificationService notificationService, MockComputationResultService resultService, ObjectMapper objectMapper, UuidGeneratorService uuidGeneratorService, String defaultProvider) {
            super(notificationService, resultService, objectMapper, uuidGeneratorService, defaultProvider);
        }

        @Override
        public List<String> getProviders() {
            return List.of();
        }

        @Override
        public UUID runAndSaveResult(MockComputationRunContext runContext) {
            return resultUuid;
        }
    }

    enum ComputationResultWanted {
        SUCCESS,
        FAIL,
        SLOW_SUCCESS
    }

    public class MockComputationWorkerService extends AbstractWorkerService<MockComputationResult, MockComputationRunContext, MockComputationParameters, MockComputationResultService> {
        protected MockComputationWorkerService(NetworkStoreService networkStoreService, NotificationService notificationService, ReportService reportService, MockComputationResultService resultService, ExecutionService executionService, AbstractComputationObserver<MockComputationResult, MockComputationParameters> observer, ObjectMapper objectMapper) {
            super(networkStoreService, notificationService, reportService, resultService, executionService, observer, objectMapper);
        }

        @Override
        protected AbstractResultContext<MockComputationRunContext> fromMessage(Message<String> message) {
            return resultContext;
        }

        @Override
        protected void saveResult(Network network, AbstractResultContext<MockComputationRunContext> resultContext, MockComputationResult result) { }

        @Override
        protected String getComputationType() {
            return COMPUTATION_TYPE;
        }

        @Override
        protected CompletableFuture<MockComputationResult> getCompletableFuture(MockComputationRunContext runContext, String provider, UUID resultUuid) {
            final CompletableFuture<MockComputationResult> completableFuture = new CompletableFuture<>();
            switch (runContext.getComputationResWanted()) {
                case FAIL:
                    completableFuture.completeExceptionally(new RuntimeException("Computation failed"));
                    break;
                case SLOW_SUCCESS:
                    ExecutorService executorService = Executors.newFixedThreadPool(1);
                    executorService.submit(() -> {
                        Thread.sleep(5000);
                        // TODO : try something like this : await().atMost(2, Duration.SECONDS).until(didTheThing());  // Compliant
                        completableFuture.complete(new MockComputationResult());
                        return COMPLETED;
                    });
                    break;
                case SUCCESS:
                    return CompletableFuture.supplyAsync(MockComputationResult::new);
            }
            return completableFuture;
        }
    }

    private MockComputationResultService resultService;
    @Mock
    private VariantManager variantManager;
    @Mock
    private MockComputationWorkerService workerService;
    @Mock
    private MockComputationService computationService;
    private MockComputationResultContext resultContext;
    final UUID networkUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
    final UUID reportUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");
    final UUID resultUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
    final String reporterId = "44444444-4444-4444-4444-444444444444";
    final String userId = "MockComputation_UserId";
    final String receiver = "MockComputation_Receiver";
    final String provider = "MockComputation_Provider";
    Message<String> message;
    MockComputationRunContext runContext;

    @BeforeEach
    void init() {
        resultService = new MockComputationResultService();
        workerService = new MockComputationWorkerService(
                networkStoreService,
                notificationService,
                reportService,
                resultService,
                executionService,
                new MockComputationObserver(ObservationRegistry.create(), new SimpleMeterRegistry()),
                objectMapper
        );
        computationService = new MockComputationService(notificationService, resultService, objectMapper, uuidGeneratorService, provider);

        MessageBuilder<String> builder = MessageBuilder
                .withPayload("")
                .setHeader(HEADER_RESULT_UUID, resultUuid.toString())
                .setHeader(HEADER_RECEIVER, receiver)
                .setHeader(HEADER_USER_ID, userId);
        message = builder.build();

        runContext = new MockComputationRunContext(networkUuid, null, receiver,
                new ReportInfos(reportUuid, reporterId, COMPUTATION_TYPE), userId, provider, new MockComputationParameters());
        runContext.setNetwork(network);
        resultContext = new MockComputationResultContext(resultUuid, runContext);
    }

    void initComputationExecution() {
        when(networkStoreService.getNetwork(eq(networkUuid), any(PreloadingStrategy.class)))
                .thenReturn(network);
        when(network.getVariantManager()).thenReturn(variantManager);
    }

    @Test
    void testComputationSuccess() {
        // inits
        initComputationExecution();
        runContext.setComputationResWanted(ComputationResultWanted.SUCCESS);

        // execution / cleaning
        workerService.consumeRun().accept(message);

        // test the course
        verify(notificationService, times(1))
                .sendResultMessage(resultUuid, receiver, userId, null);
    }

    @Test
    void testComputationFailed() {
        // inits
        initComputationExecution();
        runContext.setComputationResWanted(ComputationResultWanted.FAIL);

        // execution / cleaning
        workerService.consumeRun().accept(message);

        // test the course
        verify(notificationService, times(1))
                .publishFail(resultUuid, receiver, "java.lang.RuntimeException: Computation failed", userId, COMPUTATION_TYPE, null);
    }

    @Test
    void testComputationService() {
        MockComputationStatus baseStatus = NOT_DONE;
        computationService.setStatus(List.of(resultUuid), baseStatus);
        assertEquals(baseStatus, computationService.getStatus(resultUuid));

        computationService.stop(resultUuid, receiver);

        // test the course
        /*Message<String> cancelMessage = MessageBuilder.withPayload("")
                .setHeader(HEADER_RESULT_UUID, resultUuid.toString())
                .setHeader(HEADER_RECEIVER, receiver)
                .build();
        verify(notificationService, times(1))
                .sendCancelMessage(cancelMessage);*/
    }
}
