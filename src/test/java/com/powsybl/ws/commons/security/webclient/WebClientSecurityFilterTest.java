/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security.webclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.HEADER_ROLES;
import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.HEADER_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
class WebClientSecurityFilterTest {

    WebClientSecurityFilter securityFilter;

    @BeforeEach
    void setUp() {
        securityFilter = new WebClientSecurityFilter();
    }

    @Test
    void shouldAuthenticateUserAndRolesWhenProvided() {
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header(HEADER_USER_ID, "user1")
                .header(HEADER_ROLES, "ROLE_ADMIN|ROLE_USER")
                .build());

        securityFilter.filter(exchange, webExchange -> ReactiveSecurityContextHolder.getContext()
                .doOnNext(context -> authentication.set(context.getAuthentication()))
                .then()).block();

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getPrincipal()).isEqualTo("user1");
        assertThat(authentication.get().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void shouldAuthenticateUserWhenRolesAreMissing() {
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header(HEADER_USER_ID, "user1")
                .build());

        securityFilter.filter(exchange, webExchange -> ReactiveSecurityContextHolder.getContext()
                .doOnNext(context -> authentication.set(context.getAuthentication()))
                .then()).block();

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getPrincipal()).isEqualTo("user1");
        assertThat(authentication.get().getAuthorities()).isEmpty();
    }

    @Test
    void shouldNotAuthenticateWhenUserIdIsMissing() {
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        securityFilter.filter(exchange, webExchange -> ReactiveSecurityContextHolder.getContext()
                .doOnNext(context -> authentication.set(context.getAuthentication()))
                .then()).block();

        assertThat(authentication.get()).isNull();
    }

    @Test
    void shouldNotAuthenticateWhenUserIdIsEmpty() {
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header(HEADER_USER_ID, "")
                .build());

        securityFilter.filter(exchange, webExchange -> ReactiveSecurityContextHolder.getContext()
                .doOnNext(context -> authentication.set(context.getAuthentication()))
                .then()).block();

        assertThat(authentication.get()).isNull();
    }
}
