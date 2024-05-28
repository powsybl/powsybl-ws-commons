/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.utils;

import com.powsybl.commons.PowsyblException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class MessageUtils {
    public static final int MSG_MAX_LENGTH = 256;

    private MessageUtils() {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static String getNonNullHeader(MessageHeaders headers, String name) {
        String header = headers.get(name, String.class);
        if (header == null) {
            throw new PowsyblException("Header '" + name + "' not found");
        }
        return header;
    }

    /**
     * Prevent the message from being too long for RabbitMQ.
     * @apiNote the beginning and ending are both kept, it should make it easier to identify
     */
    public static String shortenMessage(String msg) {
        if (msg == null) {
            return null;
        }

        return StringUtils.abbreviateMiddle(msg, " ... ", MSG_MAX_LENGTH);
    }
}
