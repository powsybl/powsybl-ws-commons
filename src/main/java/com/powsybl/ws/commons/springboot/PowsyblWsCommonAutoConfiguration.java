/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.springboot;

import com.powsybl.ws.commons.error.BaseExceptionHandler;
import com.powsybl.ws.commons.security.restclient.HeaderForwardingInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@Slf4j
@AutoConfiguration
@Import(PowsyblWsCommonAutoConfiguration.TomcatConfiguration.class)
@EnableConfigurationProperties({ PowsyblWsCommonProperties.class })
public class PowsyblWsCommonAutoConfiguration {
    private final String appName;

    public PowsyblWsCommonAutoConfiguration(Environment env) {
        this.appName = env.getProperty("spring.application.name");
    }

    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "powsybl-ws.autoconfigure", name = "base-exception-handler.enable", matchIfMissing = true)
    @Bean(name = "powsyblBaseExceptionHandler")
    public BaseExceptionHandler powsyblBaseExceptionHandler() {
        return new BaseExceptionHandler(() -> appName);
    }

    @ConditionalOnClass(RestClient.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Bean(name = "headerForwardingRestClientCustomizer")
    public RestClientCustomizer restClientCustomizer() {
        return builder -> builder.requestInterceptor(new HeaderForwardingInterceptor());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication
    @ConditionalOnClass(name = { "org.apache.catalina.startup.Tomcat", "org.springframework.boot.tomcat.TomcatConnectorCustomizer" })
    @ConditionalOnProperty(prefix = "powsybl-ws.autoconfigure", name = "tomcat-customize.enable", matchIfMissing = true)
    static class TomcatConfiguration {

        @Bean(name = "powsyblTomcatConnectorCustomizer")
        TomcatConnectorCustomizer powsyblCustomizeTomcatConnector(PowsyblWsCommonProperties properties) {
            return new TomcatCustomization(properties.getTomcatCustomize());
        }
    }
}
