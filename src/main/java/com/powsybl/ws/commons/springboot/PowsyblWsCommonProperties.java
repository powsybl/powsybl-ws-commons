package com.powsybl.ws.commons.springboot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "powsybl-ws.common", ignoreInvalidFields = false, ignoreUnknownFields = false)
public class PowsyblWsCommonProperties {
    /**
     * Whether powsybl-ws-common auto-configure module/beans should be skipped.
     */
    private boolean skipInit = false;

    /**
     * Configuration specific of Tomcat (if present).
     */
    private TomcatPowsyblProperties tomcat = new TomcatPowsyblProperties();

    @Data
    public class TomcatPowsyblProperties {
        /**
         * Whether PowSyBl should auto-configure Tomcat connectors' attribute "encodedSolidusHandling"=PASS_THROUGH.
         */
        private boolean encodedSolidusHandling = true;
    }
}
