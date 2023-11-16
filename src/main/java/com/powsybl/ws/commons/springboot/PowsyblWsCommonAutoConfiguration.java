package com.powsybl.ws.commons.springboot;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.startup.Tomcat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({ PowsyblWsCommonProperties.class })
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class PowsyblWsCommonAutoConfiguration {
    private final PowsyblWsCommonProperties properties;

    @ConditionalOnWebApplication
    @ConditionalOnClass({ Tomcat.class })
    @ConditionalOnProperty(prefix = "powsybl-ws.autoconfigure", name = "tomcat-customize.enable", matchIfMissing = true)
    @Bean(name = "powsyblTomcatConnectorCustomizer")
    public TomcatConnectorCustomizer powsyblCustomizeTomcatConnector() {
        return new TomcatCustomization(properties.getTomcatCustomize());
    }
}
