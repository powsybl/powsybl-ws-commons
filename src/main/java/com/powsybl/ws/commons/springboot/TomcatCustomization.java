package com.powsybl.ws.commons.springboot;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TomcatCustomization implements TomcatConnectorCustomizer {
    /**
     * {@inheritDoc}
     */
    @Override
    public void customize(final Connector connector) {
        log.info("set EncodedSolidusHandling to PASS_THROUGH");
        connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue());
    }
}
