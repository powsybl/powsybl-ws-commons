/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.springboot;

import com.powsybl.ws.commons.springboot.PowsyblWsCommonProperties.TomcatPowsyblProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor()
public class TomcatCustomization implements TomcatConnectorCustomizer {
    private final TomcatPowsyblProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public void customize(final Connector connector) {
        if (properties.isEncodedSolidusHandling()) {
            log.info("set EncodedSolidusHandling to PASS_THROUGH");
            connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
        }
    }
}
