package com.powsybl.ws.commons;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "powsybl-ws")
public class PowsyblProperties {
    /**
     * Whether PowSyBl beans should be auto-configured.
     */
    private boolean skipInit = false;

    private TomcatPowsyblProperties tomcat = new TomcatPowsyblProperties();

    @Data
    public class TomcatPowsyblProperties {
        /**
         * Whether PowSyBl should auto-configure Tomcat connectors' attribute "encodedSolidusHandling"=PASS_THROUGH.
         */
        private boolean encodedSolidusHandling = true;
    }
}
