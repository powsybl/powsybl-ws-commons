/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail.ServerName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class PowsyblWsProblemDetailTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serializeTypedPropertiesTest() throws Exception {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        PowsyblWsProblemDetail problem = PowsyblWsProblemDetail.builder(HttpStatus.BAD_REQUEST)
            .title("Bad Request")
            .server("directory-server")
            .businessErrorCode("directory.ERROR_C0DE_01")
            .detail("invalid payload")
            .timestamp(now)
            .path("directory-server/api")
            .traceId("trace-1")
            .appendChain(PowsyblWsProblemDetail.CallContext.of(
                "directory-server",
                "POST",
                "/api",
                HttpStatus.BAD_REQUEST.value(),
                "directory.ERROR",
                "invalid payload",
                now
            ))
            .build();

        String json = OBJECT_MAPPER.writeValueAsString(problem);
        JsonNode node = OBJECT_MAPPER.readTree(json);
        assertThat(node.get("server").asText()).isEqualTo("directory-server");
        assertThat(node.get("businessErrorCode").asText()).isEqualTo("directory.ERROR_C0DE_01");
        assertThat(node.get("detail").asText()).isEqualTo("invalid payload");
        assertThat(node.get("timestamp").asText()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(node.get("path").asText()).isEqualTo("directory-server/api");
        assertThat(node.get("traceId").asText()).isEqualTo("trace-1");
        assertThat(node.get("chain")).isNotNull();
        com.fasterxml.jackson.databind.node.ArrayNode chain = (com.fasterxml.jackson.databind.node.ArrayNode) node.get("chain");
        assertThat(chain).hasSize(1);
        JsonNode context = chain.get(0);
        assertThat(context.get("server").asText()).isEqualTo("directory-server");
        assertThat(context.get("method").asText()).isEqualTo("POST");
        assertThat(context.get("path").asText()).isEqualTo("/api");
        assertThat(context.get("status").asInt()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(context.get("code").asText()).isEqualTo("directory.ERROR");
        assertThat(context.get("message").asText()).isEqualTo("invalid payload");
        assertThat(context.get("timestamp").asText()).isEqualTo("2025-01-01T00:00:00Z");
    }

    @Test
    void deserializeJsonIntoTypedProblemDetailTest() throws Exception {
        String json = """
            {
              "status": 403,
              "title": "Forbidden",
              "detail": "Access denied",
              "server": "directory-server",
              "businessErrorCode": "DIRECTORY_PERMISSION_DENIED",
              "timestamp": "2025-02-10T12:34:56Z",
              "path": "directory-server/resources",
              "traceId": "trace-id-01",
              "chain": [
                {
                  "server": "directory-server",
                  "method": "GET",
                  "path": "/resources",
                  "status": 403,
                  "code": "DIRECTORY_PERMISSION_DENIED",
                  "message": "Access denied",
                  "timestamp": "2025-02-10T12:34:56Z"
                },
                {
                  "server": "explore-server",
                  "method": "GET",
                  "path": "/cases",
                  "status": 403,
                  "code": "REMOTE_ERROR",
                  "message": "directory server answered with 403",
                  "timestamp": "2025-02-10T12:35:00Z"
                }
              ]
            }
            """;

        PowsyblWsProblemDetail problem = OBJECT_MAPPER.readValue(json, PowsyblWsProblemDetail.class);

        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getTitle()).isEqualTo("Forbidden");
        assertThat(problem.getDetail()).isEqualTo("Access denied");
        assertThat(problem.getServer()).isEqualTo(new ServerName("directory-server"));
        assertThat(problem.getBusinessErrorCode()).isEqualTo(new PowsyblWsProblemDetail.BusinessErrorCode("DIRECTORY_PERMISSION_DENIED"));
        assertThat(problem.timestamp()).contains(Instant.parse("2025-02-10T12:34:56Z"));
        assertThat(problem.path()).map(PowsyblWsProblemDetail.ErrorPath::value).contains("directory-server/resources");
        assertThat(problem.traceId()).map(PowsyblWsProblemDetail.TraceId::value).contains("trace-id-01");
        assertThat(problem.chainEntries()).hasSize(2);
        assertThat(problem.chainEntries().get(0).server()).isEqualTo(new ServerName("directory-server"));
        assertThat(problem.chainEntries().get(0).path()).isEqualTo(PowsyblWsProblemDetail.ErrorPath.of("/resources"));
        assertThat(problem.chainEntries().get(1).server()).isEqualTo(new ServerName("explore-server"));
        assertThat(problem.chainEntries().get(1).path()).isEqualTo(PowsyblWsProblemDetail.ErrorPath.of("/cases"));

    }
}
