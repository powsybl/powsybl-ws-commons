/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class ErrorResponseParserTest {

    @Test
    void parsesValidJsonPayload() {
        String payload = """
                {
                  "service": "directory-server",
                  "errorCode": "NOT_FOUND",
                  "message": "Directory element missing",
                  "status": 404,
                  "timestamp": "2024-01-01T00:00:00Z",
                  "path": "/v1/directories/1",
                  "correlationId": "corr-id"
                }
                """;

        Optional<ErrorResponse> errorResponse = ErrorResponseParser.parse(payload);

        assertThat(errorResponse).isPresent();
        ErrorResponse error = errorResponse.orElseThrow();
        assertThat(error.service()).isEqualTo("directory-server");
        assertThat(error.errorCode()).isEqualTo("NOT_FOUND");
        assertThat(error.message()).isEqualTo("Directory element missing");
        assertThat(error.status()).isEqualTo(404);
        assertThat(error.timestamp()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(error.path()).isEqualTo("/v1/directories/1");
        assertThat(error.correlationId()).isEqualTo("corr-id");
    }

    @Test
    void returnsEmptyOptionalWhenPayloadIsBlank() {
        assertThat(ErrorResponseParser.parse(" ")).isEmpty();
    }

    @Test
    void returnsEmptyOptionalWhenPayloadCannotBeParsed() {
        assertThat(ErrorResponseParser.parse("not-json")).isEmpty();
    }

    @Test
    void parsesValidBytePayload() {
        String payload = """
                {
                  "service": "explore-server",
                  "errorCode": "REMOTE_ERROR",
                  "message": "Something went wrong",
                  "status": 500,
                  "timestamp": "2024-01-02T00:00:00Z",
                  "path": "/v1/test",
                  "correlationId": "corr-2"
                }
                """;

        Optional<ErrorResponse> errorResponse = ErrorResponseParser.parse(payload.getBytes(StandardCharsets.UTF_8));

        assertThat(errorResponse).isPresent();
        assertThat(errorResponse.orElseThrow().service()).isEqualTo("explore-server");
    }
}
