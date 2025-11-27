/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

    /**
     * Configuration specific of base exception handler (if present).
     */
    private BaseExceptionHandlerProperties baseExceptionHandler = new BaseExceptionHandlerProperties();

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

    @Data
    public static class BaseExceptionHandlerProperties {
        /**
         * Enable PowSyBl autoconfiguration of base exception handler
         */
        private boolean enable = true;
    }
}
