package com.powsybl.ws.commons;

import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties({ PowsyblProperties.class })
//@Conditional({ TomcatAutoCustomizationConditions.class })
public class TomcatAutoCustomization {
    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatAutoCustomization.class);

    @Bean
    public WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> customize() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                LOGGER.info("set EncodedSolidusHandling to PASS_THROUGH");
                connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
            });
        };
    }

    static class TomcatAutoCustomizationConditions extends AllNestedConditions {
        public TomcatAutoCustomizationConditions() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnWebApplication
        static class OnWebApplication { }

        /*@ConditionalOnBean({ ConfigurableTomcatWebServerFactory.class })
        static class OnBeanTomcatWebServerFactory { }*/

        @ConditionalOnClass({ Tomcat.class })
        static class OnClassTomcat { }

        /*@ConditionalOnProperty(prefix = "powsybl-ws", name = "skip-init", havingValue = "false")
        static class OnPropertySkipInit { }

        @ConditionalOnProperty(prefix = "powsybl-ws", name = "tomcat.encodedSolidusHandling", havingValue = "true", matchIfMissing = true)
        static class OnPropertyTomcatEncodeSlashPassthrough { }*/
    }
}
