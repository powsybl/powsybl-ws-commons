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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Instant;
import java.util.Optional;

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

    private String serverName() {
        return serverNameProvider.serverName();
    }

    protected abstract Optional<PowsyblWsProblemDetail> getRemoteError(E ex);

    protected abstract C getBusinessCode(E ex);

    protected abstract HttpStatus mapStatus(C code);

    protected abstract E wrapRemote(PowsyblWsProblemDetail remoteError);

    protected abstract C defaultRemoteErrorCode();

    @ExceptionHandler(HttpStatusCodeException.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleRemoteException(
        HttpStatusCodeException exception, HttpServletRequest request) {

        E wrapped = mapRemoteException(exception);
        return handleDomainException(wrapped, request);
    }

    @ExceptionHandler(AbstractBusinessException.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleDomainException(
        E exception, HttpServletRequest request) {

        HttpStatus status = resolveStatus(exception);
        PowsyblWsProblemDetail body = getRemoteError(exception)
            .map(remote -> buildFromRemoteException(request, status, exception, remote))
            .orElseGet(() -> buildFromLocalException(request, status, exception));

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleAllExceptions(
        Exception exception, HttpServletRequest request) {
        LOGGER.error(exception.getMessage(), exception);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = firstNonBlank(exception.getMessage(), status.getReasonPhrase());
        return ResponseEntity.status(status)
            .body(buildFromGenericException(request, status, message));
    }

    private PowsyblWsProblemDetail buildFromRemoteException(
        HttpServletRequest request,
        HttpStatus status,
        E exception,
        PowsyblWsProblemDetail remoteError) {

        String localBusiness = getBusinessCode(exception).value();
        String business = firstNonBlank(remoteError.getBusinessErrorCode(), localBusiness);

        String message = firstNonBlank(remoteError.getDetail(), exception.getMessage(), status.getReasonPhrase());

        Instant now = Instant.now();
        String method = request.getMethod();
        String path = Optional.ofNullable(remoteError.getPath()).orElse(serverName());

        PowsyblWsProblemDetail.Builder b = PowsyblWsProblemDetail.builderFrom(remoteError)
            .title(status.getReasonPhrase())
            .detail(message)
            .appendChain(serverName(), method, path, status.value(), now);

        if (remoteError.getBusinessErrorCode() == null && business != null) {
            b.businessErrorCode(business);
        }
        return b.build();
    }

    private PowsyblWsProblemDetail buildFromLocalException(
        HttpServletRequest request,
        HttpStatus status,
        E exception) {

        String business = getBusinessCode(exception).value();
        String message = firstNonBlank(exception.getMessage(), status.getReasonPhrase());

        return baseBuilder(request, status)
            .businessErrorCode(business)
            .detail(message)
            .build();
    }

    private PowsyblWsProblemDetail buildFromGenericException(
        HttpServletRequest request, HttpStatus status, String message) {

        String effectiveMessage = firstNonBlank(message, status.getReasonPhrase());
        return baseBuilder(request, status)
            .businessErrorCode(null)
            .detail(effectiveMessage)
            .build();
    }

    private PowsyblWsProblemDetail.Builder baseBuilder(HttpServletRequest request, HttpStatus status) {
        return PowsyblWsProblemDetail.builder(status)
            .title(status.getReasonPhrase())
            .server(serverName())
            .timestamp(Instant.now())
            .path(request.getRequestURI());
    }

    private HttpStatus resolveStatus(E exception) {
        return getRemoteError(exception)
            .map(PowsyblWsProblemDetail::getStatus)
            .map(HttpStatus::resolve) // remote provided an int code
            .orElseGet(() -> mapStatus(getBusinessCode(exception)));
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    protected E mapRemoteException(HttpStatusCodeException ex) {
        try {
            byte[] body = ex.getResponseBodyAsByteArray();
            PowsyblWsProblemDetail remote =
                OBJECT_MAPPER.readValue(body, PowsyblWsProblemDetail.class);
            return wrapRemote(remote);
        } catch (Exception ignored) {
            return wrapRemote(fallbackRemoteError(ex));
        }
    }

    private PowsyblWsProblemDetail fallbackRemoteError(HttpStatusCodeException ex) {
        Instant now = Instant.now();
        return PowsyblWsProblemDetail.builder(ex.getStatusCode())
            .server(serverName())
            .businessErrorCode(defaultRemoteErrorCode().value())
            .detail(ex.getMessage())
            .timestamp(now)
            .build();
    }
}
