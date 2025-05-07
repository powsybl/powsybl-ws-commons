/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.ws.commons.StreamUtils;
import com.powsybl.ws.commons.computation.utils.StreamerWithInfos;
import lombok.Getter;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static com.powsybl.ws.commons.StreamUtils.DEFAULT_BUFFER_SIZE;
import static com.powsybl.ws.commons.computation.service.S3Service.METADATA_ORIGINAL_FILENAME;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france.com>
 * @param <C> run context specific to a computation, including parameters
 * @param <T> run service specific to a computation
 * @param <S> enum status specific to a computation
 */
public abstract class AbstractComputationService<C extends AbstractComputationRunContext<?>, T extends AbstractComputationResultService<S>, S> {

    protected ObjectMapper objectMapper;
    protected NotificationService notificationService;
    protected UuidGeneratorService uuidGeneratorService;
    protected T resultService;
    protected Optional<S3Service> s3Service;
    @Getter
    private final String defaultProvider;

    protected AbstractComputationService(NotificationService notificationService,
                                         T resultService,
                                         Optional<S3Service> s3Service,
                                         ObjectMapper objectMapper,
                                         UuidGeneratorService uuidGeneratorService,
                                         String defaultProvider) {
        this.notificationService = Objects.requireNonNull(notificationService);
        this.objectMapper = objectMapper;
        this.uuidGeneratorService = Objects.requireNonNull(uuidGeneratorService);
        this.defaultProvider = defaultProvider;
        this.resultService = Objects.requireNonNull(resultService);
        this.s3Service = s3Service;
    }

    public void stop(UUID resultUuid, String receiver) {
        notificationService.sendCancelMessage(new CancelContext(resultUuid, receiver).toMessage());
    }

    public void stop(UUID resultUuid, String receiver, String userId) {
        notificationService.sendCancelMessage(new CancelContext(resultUuid, receiver, userId).toMessage());
    }

    public abstract List<String> getProviders();

    public abstract UUID runAndSaveResult(C runContext);

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

    public StreamerWithInfos getDebugFileStreamer(UUID resultUuid) throws IOException {
        if (s3Service.isPresent()) {
            String debugFileLocation = resultService.findDebugFileLocation(resultUuid);
            ResponseInputStream<GetObjectResponse> inputStream;
            inputStream = s3Service.get().downloadFile(debugFileLocation);
            String fileName = inputStream.response().metadata().get(METADATA_ORIGINAL_FILENAME);
            Long fileLength = inputStream.response().contentLength();
            Consumer<OutputStream> streamer = StreamUtils.getStreamer(inputStream, DEFAULT_BUFFER_SIZE);
            return StreamerWithInfos.builder()
                    .streamer(streamer)
                    .fileName(fileName)
                    .fileLength(fileLength)
                    .build();
        }
        throw new IOException("No s3 supported");
    }

}
