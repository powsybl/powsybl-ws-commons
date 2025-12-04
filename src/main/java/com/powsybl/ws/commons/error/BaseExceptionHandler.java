/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
@ControllerAdvice
public class BaseExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionHandler.class);

    private final ServerNameProvider serverNameProvider;

    public BaseExceptionHandler(ServerNameProvider serverNameProvider) {
        this.serverNameProvider = serverNameProvider;
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<PowsyblWsProblemDetail> handleRemoteException(
        HttpStatusCodeException exception, HttpServletRequest request) {

        PowsyblWsProblemDetail problemDetail = ErrorUtils.extractProblemDetail(serverNameProvider.serverName(), exception, request);
        problemDetail.wrap(serverNameProvider.serverName(), request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(exception.getStatusCode()).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PowsyblWsProblemDetail> handleAllExceptions(
        Exception exception, HttpServletRequest request) {

        if (exception instanceof ErrorResponse errorResponse) {
            PowsyblWsProblemDetail problemDetail = new PowsyblWsProblemDetail(
                errorResponse.getBody(),
                serverNameProvider.serverName(),
                request.getRequestURI()
            );
            problemDetail.wrap(serverNameProvider.serverName(), request.getMethod(), request.getRequestURI());
            return ResponseEntity.status(errorResponse.getStatusCode()).body(problemDetail);
        }
        LOGGER.error(exception.getMessage(), exception);
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        PowsyblWsProblemDetail problemDetail = ErrorUtils.baseBuilder(serverNameProvider.serverName(), status, request)
            .detail(exception.getMessage()).build();
        return ResponseEntity.status(status).body(problemDetail);
    }
}
