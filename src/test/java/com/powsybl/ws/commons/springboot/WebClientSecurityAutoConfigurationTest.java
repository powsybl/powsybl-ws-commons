/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.springboot;

import com.powsybl.ws.commons.security.webclient.WebClientAuthorizationService;
import com.powsybl.ws.commons.security.webclient.WebClientSecurityFilter;
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
class WebClientSecurityAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebClientSecurityAutoConfiguration.class))
            .withUserConfiguration(WebClientBuilderConfiguration.class);

    @Test
    void shouldConfigureReactiveSecurityWhenEnabled() {
        reactiveContextRunner.withPropertyValues("powsybl-ws.autoconfigure.authorization-system.enable=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(WebClientSecurityAutoConfiguration.class);
                    assertThat(context).hasSingleBean(WebClientAuthorizationService.class);
                    assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
                    assertThat(context.getBean(SecurityWebFilterChain.class).getWebFilters().collectList().block())
                            .anyMatch(WebClientSecurityFilter.class::isInstance);
                });
    }

    @Test
    void shouldNotConfigureReactiveSecurityWhenDisabled() {
        reactiveContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WebClientSecurityAutoConfiguration.class);
            assertThat(context).doesNotHaveBean(WebClientAuthorizationService.class);
            assertThat(context).doesNotHaveBean(SecurityWebFilterChain.class);
        });
    }

    @Test
    void shouldNotConfigureReactiveSecurityInNonWebContext() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebClientSecurityAutoConfiguration.class))
                .withPropertyValues("powsybl-ws.autoconfigure.authorization-system.enable=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WebClientSecurityAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(WebClientAuthorizationService.class);
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
