/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france.com>
 * @param <R> run context specific to a computation, including parameters
 * @param <T> run service specific to a computation
 * @param <S> enum status specific to a computation
 */
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractComputationService<R extends AbstractComputationRunContext<?>, T extends AbstractComputationResultService<S>, S> {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final UuidGeneratorService uuidGeneratorService;
    private final T resultService;
    @Getter
    private final String defaultProvider;

    protected AbstractComputationService(NotificationService notificationService,
                                         T resultService,
                                         ObjectMapper objectMapper,
                                         UuidGeneratorService uuidGeneratorService,
                                         String defaultProvider) {
        this.notificationService = Objects.requireNonNull(notificationService);
        this.objectMapper = objectMapper;
        this.uuidGeneratorService = Objects.requireNonNull(uuidGeneratorService);
        this.defaultProvider = defaultProvider;
        this.resultService = Objects.requireNonNull(resultService);
    }

    public void stop(UUID resultUuid, String receiver) {
        notificationService.sendCancelMessage(new CancelContext(resultUuid, receiver).toMessage());
    }

    public abstract List<String> getProviders();

    public abstract UUID runAndSaveResult(R runContext);

    public void deleteResult(UUID resultUuid) {
        resultService.delete(resultUuid);
    }

    public void deleteResults(List<UUID> resultUuids) {
        if (!CollectionUtils.isEmpty(resultUuids)) {
            resultUuids.forEach(resultService::delete);
        } else {
            deleteResults();
        }
    }

    public void deleteResults() {
        resultService.deleteAll();
    }

    public void setStatus(List<UUID> resultUuids, S status) {
        resultService.insertStatus(resultUuids, status);
    }

    public S getStatus(UUID resultUuid) {
        return resultService.findStatus(resultUuid);
    }
}
