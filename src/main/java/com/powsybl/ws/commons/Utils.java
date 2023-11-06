/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public final class Utils {

    private static final String ALLOW_ENCODED_SLASH_IN_PATH_PROPERTY = "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH";

    private Utils() {
    }

    /**
     * @deprecated for Tomcat 9, not Tomcat 10 with SpringBoot3
     */
    @Deprecated(since = "1.7.0")
    public static void initProperties() {
        String powsyblWsSkipInitProperties = System.getProperty("powsyblWsSkipInitProperties");
        if (powsyblWsSkipInitProperties != null && !powsyblWsSkipInitProperties.equals("false")) {
            return;
        }

        System.setProperty(ALLOW_ENCODED_SLASH_IN_PATH_PROPERTY, "true");
    }
}
