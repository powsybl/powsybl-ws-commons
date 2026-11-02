/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static com.powsybl.ws.commons.security.UserContext.*;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */

/**
 * In our microservice architecture, user permissions (userId and roles) must be preserved when
 * one service calls another. This interceptor automatically forwards the "userId" and "roles" headers
 * from incoming requests to any outgoing REST calls made with this RestClient.
 *
 * Without this interceptor, authorization information would be lost in service-to-service
 * communication.
 */
public class HeaderForwardingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set(HEADER_USER_ID, UserContext.getUserId());
        String roles = UserContext.getRoles();
        if (roles != null && !roles.isEmpty()) {
            request.getHeaders().set(HEADER_ROLES, roles);
        }

        return execution.execute(request, body);
    }
}
