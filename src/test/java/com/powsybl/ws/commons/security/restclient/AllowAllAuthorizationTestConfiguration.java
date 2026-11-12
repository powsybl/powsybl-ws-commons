/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security.restclient;

import com.powsybl.ws.commons.security.webclient.WebClientAuthorizationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@TestConfiguration
public class AllowAllAuthorizationTestConfiguration {

    @Bean(name = "authorizationService")
    @Primary
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RestClientAuthorizationService restClientAuthorizationService() {
        return mock(
                RestClientAuthorizationService.class,
                invocation -> true
        );
    }

    @Bean(name = "authorizationService")
    @Primary
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public WebClientAuthorizationService webClientAuthorizationService() {
        return mock(
                WebClientAuthorizationService.class,
                invocation -> Mono.just(true)
        );
    }
}
