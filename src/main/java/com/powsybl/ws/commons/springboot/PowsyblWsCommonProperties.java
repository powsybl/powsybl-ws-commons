package com.powsybl.ws.commons.springboot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "powsybl-ws.autoconfigure")
public class PowsyblWsCommonProperties {
    /**
     * Configuration specific of Tomcat (if present).
     */
    private TomcatPowsyblProperties tomcatCustomize = new TomcatPowsyblProperties();

    @Data
    public static class TomcatPowsyblProperties {
        /**
         * Enable PowSyBl autoconfiguration of Tomcat
         */
        private boolean enable = true;

        /**
         * Whether PowSyBl should auto-configure Tomcat connectors' attribute "encodedSolidusHandling"=PASS_THROUGH.
         */
        private boolean encodedSolidusHandling = true;
    }
}
