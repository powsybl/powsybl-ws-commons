/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.ws.commons.s3.S3InputStreamInfos;
import com.powsybl.ws.commons.s3.S3Service;
import lombok.Getter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    protected S3Service s3Service;
    @Getter
    private final String defaultProvider;

    protected AbstractComputationService(NotificationService notificationService,
                                         T resultService,
                                         ObjectMapper objectMapper,
                                         UuidGeneratorService uuidGeneratorService,
                                         String defaultProvider) {
        this(notificationService, resultService, null, objectMapper, uuidGeneratorService, defaultProvider);
    }

    protected AbstractComputationService(NotificationService notificationService,
                                         T resultService,
                                         S3Service s3Service,
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

    public ResponseEntity<Resource> downloadDebugFile(UUID resultUuid) {
        if (s3Service == null) {
            throw new PowsyblException("S3 service not available");
        }

        String s3Key = resultService.findDebugFileLocation(resultUuid);
        if (s3Key == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            S3InputStreamInfos s3InputStreamInfos = s3Service.downloadFile(s3Key);
            InputStream inputStream = s3InputStreamInfos.getInputStream();
            String fileName = s3InputStreamInfos.getFileName();
            Long fileLength = s3InputStreamInfos.getFileLength();

            // build header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());
            headers.setContentLength(fileLength);

            // wrap s3 input stream
            InputStreamResource resource = new InputStreamResource(inputStream);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
