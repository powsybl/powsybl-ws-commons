package com.powsybl.ws.commons.springboot;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "powsybl-ws.common", name = "skip-init", havingValue = "false", matchIfMissing = true)
@EnableConfigurationProperties({ PowsyblWsCommonProperties.class })
public class PowsyblWsCommonAutoConfiguration {
    @ConditionalOnWebApplication
    @ConditionalOnClass({ Tomcat.class })
    @ConditionalOnProperty(prefix = "powsybl-ws.common", name = "tomcat.encoded-solidus-handling", havingValue = "true", matchIfMissing = true)
    @Bean
    public WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> powsyblCustomizeTomcatFactory() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                log.info("set EncodedSolidusHandling to PASS_THROUGH");
                connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
            });
        };
    }

    /*@ConditionalOnWebApplication
    @ConditionalOnClass({ Tomcat.class })
    @ConditionalOnProperty(prefix = "powsybl-ws.common", name = "tomcat.encoded-solidus-handling", havingValue = "true", matchIfMissing = true)
    @Bean
    public TomcatConnectorCustomizer powsyblCustomizeTomcatConnector() {
        return new TomcatCustomization();
    }*/
}
