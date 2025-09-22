/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Optional;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 *
 * Utility class used to deserialize {@link ErrorResponse} payloads.
 */
public final class ErrorResponseParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ErrorResponseParser() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static Optional<ErrorResponse> parse(String payload) {
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.readValue(payload, ErrorResponse.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public static Optional<ErrorResponse> parse(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.readValue(payload, ErrorResponse.class));
        } catch (java.io.IOException e) {
            return Optional.empty();
        }
    }
}
