/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
public final class ErrorUtils {

    private ErrorUtils() {
        // Should not be instantiated
    }

    public static PowsyblWsProblemDetail.Builder baseBuilder(
        String serverName, HttpStatusCode status, HttpServletRequest request) {
        return PowsyblWsProblemDetail.builder(status).server(serverName).path(request.getRequestURI());
    }

    public static PowsyblWsProblemDetail extractProblemDetail(
        String serverName, HttpStatusCodeException exception, HttpServletRequest request) {

        try {
            byte[] body = exception.getResponseBodyAsByteArray();
            return PowsyblWsProblemDetail.fromBytes(body);
        } catch (Exception ignored) {
            return baseBuilder(serverName, exception.getStatusCode(), request).detail(exception.getMessage()).build();
        }
    }
}
