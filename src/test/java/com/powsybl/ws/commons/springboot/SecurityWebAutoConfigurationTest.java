/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.springboot;

import com.powsybl.ws.commons.security.AuthorizationWebService;
import com.powsybl.ws.commons.security.SecurityWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
class SecurityWebAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityWebAutoConfiguration.class))
            .withUserConfiguration(WebClientBuilderConfiguration.class);

    @Test
    void shouldConfigureReactiveSecurityWhenEnabled() {
        reactiveContextRunner.withPropertyValues("powsybl-ws.autoconfigure.authorization-system.enable=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecurityWebAutoConfiguration.class);
                    assertThat(context).hasSingleBean(AuthorizationWebService.class);
                    assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
                    assertThat(context.getBean(SecurityWebFilterChain.class).getWebFilters().collectList().block())
                            .anyMatch(SecurityWebFilter.class::isInstance);
                });
    }

    @Test
    void shouldNotConfigureReactiveSecurityWhenDisabled() {
        reactiveContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SecurityWebAutoConfiguration.class);
            assertThat(context).doesNotHaveBean(AuthorizationWebService.class);
            assertThat(context).doesNotHaveBean(SecurityWebFilterChain.class);
        });
    }

    @Test
    void shouldNotConfigureReactiveSecurityInNonWebContext() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SecurityWebAutoConfiguration.class))
                .withPropertyValues("powsybl-ws.autoconfigure.authorization-system.enable=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SecurityWebAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(AuthorizationWebService.class);
                    assertThat(context).doesNotHaveBean(SecurityWebFilterChain.class);
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class WebClientBuilderConfiguration {
        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }
}
