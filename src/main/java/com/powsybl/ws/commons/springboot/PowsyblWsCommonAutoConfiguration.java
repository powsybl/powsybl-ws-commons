package com.powsybl.ws.commons.springboot;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "powsybl-ws.common", name = "skip-init", havingValue = "false", matchIfMissing = true)
@EnableConfigurationProperties({ PowsyblWsCommonProperties.class })
public class PowsyblWsCommonAutoConfiguration {
    /*@ConditionalOnWebApplication
    @ConditionalOnClass({ Tomcat.class })
    @ConditionalOnProperty(prefix = "powsybl-ws.common", name = "tomcat.encoded-solidus-handling", matchIfMissing = true)
    @Bean
    public WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> powsyblCustomizeTomcatFactory() {
        if (!config.getTomcat().isEncodedSolidusHandling()) {
            return null;
        } else {
            return factory -> {
                log.info("Add hook to Tomcat connector customizer");
                factory.addConnectorCustomizers(new TomcatCustomization());
            };
        }
    }*/

    @ConditionalOnWebApplication
    @ConditionalOnClass({ Tomcat.class })
    @ConditionalOnProperty(prefix = "powsybl-ws.common", name = "tomcat.encoded-solidus-handling", matchIfMissing = true)
    @Bean
    public TomcatConnectorCustomizer powsyblCustomizeTomcatConnector() {
        return new TomcatCustomization();
    }
}
