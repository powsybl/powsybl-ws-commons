/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.service;

import com.powsybl.ws.commons.computation.utils.MessageUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Objects;
import java.util.UUID;

import static com.powsybl.ws.commons.computation.service.NotificationService.*;


/**
 * @author Anis Touri <anis.touri at rte-france.com>
 */
public record CancelContext(UUID resultUuid, String receiver, String userId) {

    public CancelContext(UUID resultUuid, String receiver, String userId) {
        this.resultUuid = Objects.requireNonNull(resultUuid);
        this.receiver = Objects.requireNonNull(receiver);
        this.userId = userId;
    }

    public CancelContext(UUID resultUuid, String receiver) {
        this(resultUuid, receiver, null);
    }

    public static CancelContext fromMessage(Message<String> message) {
        Objects.requireNonNull(message);
        MessageHeaders headers = message.getHeaders();
        UUID resultUuid = UUID.fromString(MessageUtils.getNonNullHeader(headers, HEADER_RESULT_UUID));
        String receiver = headers.get(HEADER_RECEIVER, String.class);
        String userId = headers.get(HEADER_USER_ID, String.class);
        return new CancelContext(resultUuid, receiver, userId);
    }

    public Message<String> toMessage() {
        return MessageBuilder.withPayload("")
                .setHeader(HEADER_RESULT_UUID, resultUuid.toString())
                .setHeader(HEADER_RECEIVER, receiver)
                .setHeader(HEADER_USER_ID, userId)
                .build();
    }
}
