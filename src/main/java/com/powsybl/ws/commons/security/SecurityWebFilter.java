/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

import static com.powsybl.ws.commons.security.UserContext.*;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
public class SecurityWebFilter extends AuthenticationWebFilter {

    public SecurityWebFilter() {
        ReactiveAuthenticationManager authenticationManager = Mono::just;
        super(authenticationManager);
        ServerAuthenticationConverter converter = exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
            if (userId == null || userId.isEmpty()) {
                return Mono.empty();
            }
            String headerRoles = exchange.getRequest().getHeaders().getFirst(HEADER_ROLES);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, UserContext.extractRoles(headerRoles));
            return Mono.just(authentication);
        };
        setServerAuthenticationConverter(converter);
    }
}
