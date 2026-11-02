/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.springboot;

import com.powsybl.ws.commons.security.AuthorizationWebService;
import com.powsybl.ws.commons.security.SecurityWebFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "powsybl-ws.autoconfigure", name = "user-context.enable")
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(proxyTargetClass = true) // proxyTargetClass=true is required: WebSocketConfiguration injects the concrete handler classes
@EnableConfigurationProperties({ PowsyblWsCommonProperties.class })
public class SecurityWebAutoConfiguration {
    private final PowsyblWsCommonProperties properties;

    public SecurityWebAutoConfiguration(PowsyblWsCommonProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "authorizationService")
    public AuthorizationWebService authorizationService(WebClient.Builder webClientBuilder) {
        return new AuthorizationWebService(webClientBuilder,
                properties.getAuthorizationChecker().getDirectoryServerBaseUri(),
                properties.getAuthorizationChecker().getElementsAuthorizedPath());
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.addFilterAt(new SecurityWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }
}
