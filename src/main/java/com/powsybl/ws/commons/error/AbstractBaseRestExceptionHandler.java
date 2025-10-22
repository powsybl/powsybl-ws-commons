/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * @param <E> domain exception type (must extend AbstractPowsyblWsException)
 * @param <C> business error code type (must implement BusinessErrorCode)
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * <p>
 * Reusable, typed base for mapping and wrapping exceptions to PowsyblWsProblemDetail.
 */
public abstract class AbstractBaseRestExceptionHandler<E extends AbstractBusinessException, C extends BusinessErrorCode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseRestExceptionHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ServerNameProvider serverNameProvider;

    protected AbstractBaseRestExceptionHandler(ServerNameProvider serverNameProvider) {
        this.serverNameProvider = serverNameProvider;
    }

    protected abstract @NonNull C getBusinessCode(E ex);

    protected abstract HttpStatus mapStatus(C code);

    @ExceptionHandler(HttpStatusCodeException.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleRemoteException(
        HttpStatusCodeException exception, HttpServletRequest request) {

        PowsyblWsProblemDetail problemDetail = extractProblemDetail(exception, request);
        problemDetail.wrap(serverNameProvider.serverName(), request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(exception.getStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(AbstractBusinessException.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleDomainException(
        E exception, HttpServletRequest request) {

        LOGGER.warn(exception.getMessage(), exception);
        HttpStatusCode status = mapStatus(getBusinessCode(exception));
        PowsyblWsProblemDetail problemDetail = baseBuilder(status, request)
            .businessErrorCode(exception.getBusinessErrorCode().value())
            .detail(exception.getMessage())
            .build();
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleAllExceptions(
        Exception exception, HttpServletRequest request) {

        LOGGER.error(exception.getMessage(), exception);
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        PowsyblWsProblemDetail problemDetail = baseBuilder(status, request).detail(exception.getMessage()).build();
        return ResponseEntity.status(status).body(problemDetail);
    }

    private PowsyblWsProblemDetail.Builder baseBuilder(
        HttpStatusCode status, HttpServletRequest request) {

        return PowsyblWsProblemDetail.builder(status).server(serverNameProvider.serverName()).path(request.getRequestURI());
    }

    protected PowsyblWsProblemDetail extractProblemDetail(
        HttpStatusCodeException exception, HttpServletRequest request) {

        try {
            byte[] body = exception.getResponseBodyAsByteArray();
            return OBJECT_MAPPER.readValue(body, PowsyblWsProblemDetail.class);
        } catch (Exception ignored) {
            return baseBuilder(exception.getStatusCode(), request).detail(exception.getMessage()).build();
        }
    }
}
