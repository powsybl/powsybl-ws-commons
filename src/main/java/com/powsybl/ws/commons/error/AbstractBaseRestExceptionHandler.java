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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.Instant;
import java.util.Optional;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 *
 * Reusable, typed base for mapping and wrapping exceptions to PowsyblWsProblemDetail.
 *
 * @param <E> domain exception type (must extend AbstractPowsyblWsException)
 * @param <C> business error code type (must implement BusinessErrorCode)
 */
public abstract class AbstractBaseRestExceptionHandler<E extends AbstractPowsyblWsException, C extends BusinessErrorCode> {

    private final ServerNameProvider serverNameProvider;

    private static final String DEFAULT_REMOTE_ERROR_MESSAGE =
        "An unexpected error occurred while calling a remote server";

    protected final ObjectMapper objectMapper =
        new ObjectMapper().registerModule(new JavaTimeModule());

    protected AbstractBaseRestExceptionHandler(ServerNameProvider serverNameProvider) {
        this.serverNameProvider = serverNameProvider;
    }

    private String serverName() {
        return serverNameProvider.serverName();
    }

    protected abstract Optional<PowsyblWsProblemDetail> getRemoteError(E ex);

    protected abstract Optional<C> getBusinessCode(E ex);

    protected abstract HttpStatus mapStatus(C code);

    protected abstract E wrapRemote(PowsyblWsProblemDetail remoteError);

    protected abstract C defaultRemoteErrorCode();

    @ExceptionHandler(HttpStatusCodeException.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleRemoteException(
        HttpStatusCodeException exception, HttpServletRequest request) {

        E wrapped = mapRemoteException(exception);
        return handleDomainException(wrapped, request);
    }

    @ExceptionHandler
    protected ResponseEntity<PowsyblWsProblemDetail> handleDomainException(
        E exception, HttpServletRequest request) {

        HttpStatus status = resolveStatus(exception);
        return ResponseEntity.status(status)
            .body(buildErrorResponse(request, status, exception));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleAllExceptions(
        Exception exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = firstNonBlank(exception.getMessage(), status.getReasonPhrase());
        return ResponseEntity.status(status)
            .body(buildErrorResponse(request, status, null, message));
    }

    private PowsyblWsProblemDetail buildErrorResponse(
        HttpServletRequest request, HttpStatus status, E exception) {

        C currentBusinessCode = getBusinessCode(exception).orElse(null);
        PowsyblWsProblemDetail remoteError = getRemoteError(exception).orElse(null);

        String businessErrorCode =
            (remoteError != null && remoteError.getBusinessErrorCode() != null)
                ? remoteError.getBusinessErrorCode().value()
                : (currentBusinessCode != null ? currentBusinessCode.value() : null);

        String message = firstNonBlank(
            remoteError != null ? remoteError.getDetail() : null,
            exception.getMessage(),
            status.getReasonPhrase()
        );

        Instant now = Instant.now();
        String localPath = request.getRequestURI();
        String method = request.getMethod();

        PowsyblWsProblemDetail.Builder builder;
        if (remoteError != null) {
            String path = remoteError.path()
                .map(PowsyblWsProblemDetail.ErrorPath::value)
                .orElse(localPath);

            builder = PowsyblWsProblemDetail.builderFrom(remoteError)
                .title(status.getReasonPhrase())
                .detail(message)
                .appendChain(serverName(), method, path, status.value(), now);

            // If the remote error has no business code, propagate the local one if available
            if (remoteError.getBusinessErrorCode() == null && businessErrorCode != null) {
                builder.businessErrorCode(businessErrorCode);
            }
        } else {
            builder = PowsyblWsProblemDetail.builder(status)
                .title(status.getReasonPhrase())
                .server(serverName())
                .businessErrorCode(businessErrorCode)
                .detail(message)
                .timestamp(now)
                .path(localPath);
        }
        return builder.build();
    }

    private PowsyblWsProblemDetail buildErrorResponse(
        HttpServletRequest request, HttpStatus status, String businessErrorCode, String message) {

        Instant now = Instant.now();
        String effectiveMessage = firstNonBlank(message, status.getReasonPhrase());

        return PowsyblWsProblemDetail.builder(status)
            .title(status.getReasonPhrase())
            .server(serverName())
            .businessErrorCode(businessErrorCode)
            .detail(effectiveMessage)
            .timestamp(now)
            .path(request.getRequestURI())
            .build();
    }

    private HttpStatus resolveStatus(E exception) {
        return getRemoteError(exception)
            .map(PowsyblWsProblemDetail::getStatus)
            .map(this::resolveStatus)
            .orElseGet(() -> getBusinessCode(exception)
                .map(this::mapStatus)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private HttpStatus resolveStatus(Integer code) {
        if (code == null) {
            return null;
        }
        try {
            return Optional.ofNullable(HttpStatus.resolve(code))
                .orElseGet(() -> HttpStatus.valueOf(code));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    protected String codeValue(C code) {
        return code.value();
    }

    protected E mapRemoteException(HttpStatusCodeException ex) {
        try {
            byte[] body = ex.getResponseBodyAsByteArray();
            PowsyblWsProblemDetail remote =
                objectMapper.readValue(body, PowsyblWsProblemDetail.class);
            return wrapRemote(remote);
        } catch (Exception ignored) {
            return wrapRemote(fallbackRemoteError());
        }
    }

    private PowsyblWsProblemDetail fallbackRemoteError() {
        Instant now = Instant.now();
        return PowsyblWsProblemDetail.builder(HttpStatus.INTERNAL_SERVER_ERROR)
            .server(serverName())
            .businessErrorCode(codeValue(defaultRemoteErrorCode()))
            .detail(DEFAULT_REMOTE_ERROR_MESSAGE)
            .timestamp(now)
            .path(serverName())
            .build();
    }
}
