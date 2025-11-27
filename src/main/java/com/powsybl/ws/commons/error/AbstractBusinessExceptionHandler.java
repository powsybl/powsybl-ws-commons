/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/**
 * @param <E> domain exception type (must extend AbstractPowsyblWsException)
 * @param <C> business error code type (must implement BusinessErrorCode)
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * <p>
 * Reusable, typed base for mapping and wrapping business exceptions to PowsyblWsProblemDetail.
 */
public abstract class AbstractBusinessExceptionHandler<E extends AbstractBusinessException, C extends BusinessErrorCode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBusinessExceptionHandler.class);

    private final ServerNameProvider serverNameProvider;

    protected AbstractBusinessExceptionHandler(ServerNameProvider serverNameProvider) {
        this.serverNameProvider = serverNameProvider;
    }

    protected abstract @NonNull C getBusinessCode(E ex);

    protected abstract HttpStatus mapStatus(C code);

    protected ResponseEntity<PowsyblWsProblemDetail> handleDomainException(E exception, HttpServletRequest request) {
        LOGGER.warn(exception.getMessage(), exception);
        HttpStatusCode status = mapStatus(getBusinessCode(exception));
        PowsyblWsProblemDetail problemDetail = ErrorUtils.baseBuilder(serverNameProvider.serverName(), status, request)
            .businessErrorCode(exception.getBusinessErrorCode().value())
            .businessErrorValues(exception.getBusinessErrorValues())
            .detail(exception.getMessage())
            .build();
        return ResponseEntity.status(status).body(problemDetail);
    }
}
