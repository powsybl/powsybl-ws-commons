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
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail.ChainEntry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.slf4j.MDC;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class PowsyblWsProblemDetailTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serializesTypedProperties() throws Exception {
        PowsyblWsProblemDetail problem = PowsyblWsProblemDetail.builder(HttpStatus.BAD_REQUEST)
            .server("directory-server")
            .businessErrorCode("directory.ERROR")
            .detail("invalid payload")
            .path("/directory-server/api")
            .build();

        String json = OBJECT_MAPPER.writeValueAsString(problem);
        JsonNode node = OBJECT_MAPPER.readTree(json);
        assertEquals("directory-server", node.get("server").asText());
        assertEquals("directory.ERROR", node.get("businessErrorCode").asText());
        assertEquals("invalid payload", node.get("detail").asText());
        assertEquals("/directory-server/api", node.get("path").asText());
        assertNotNull(node.get("chain"));
        assertNotNull(node.get("chain"));
    }

    @Test
    void builderAddsTraceIdFromMdc() {
        MDC.put("traceId", "traceId");
        try {
            PowsyblWsProblemDetail problem = PowsyblWsProblemDetail.builder(HttpStatus.BAD_REQUEST)
                .server("directory-server")
                .businessErrorCode("directory.ERROR")
                .detail("invalid payload")
                .path("/directory-server/api")
                .build();

            assertEquals("traceId", problem.getTraceId());
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    void builderFromKeepsExistingChainWithoutDuplicating() {
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.FORBIDDEN)
            .server("c-server")
            .businessErrorCode("REMOTE_DENIED")
            .detail("upstream failure")
            .path("/c/resources")
            .build();

        remote.wrap("b-server", "GET", "/c/resources");
        remote.wrap("a-server", "GET", "/b/resources");

        assertThat(remote.getChain()).hasSize(2);
        ChainEntry first = remote.getChain().get(0);
        ChainEntry second = remote.getChain().get(1);
        assertEquals("a-server", first.fromServer());
        assertEquals("b-server", first.toServer());
        assertEquals("GET", first.method());
        assertEquals("/b/resources", first.path());
        assertEquals("b-server", second.fromServer());
        assertEquals("c-server", second.toServer());
        assertEquals("GET", second.method());
        assertEquals("/c/resources", second.path());
    }

    @Test
    void deserializesIntoTypedAccessors() throws Exception {
        String json = """
            {
              "status": 403,
              "title": "Forbidden",
              "detail": "Access denied",
              "server": "C",
              "businessErrorCode": "PERMISSION_DENIED",
              "timestamp": "2025-02-10T12:35:00Z",
              "path": "/c/resources",
              "traceId": "cid-77",
              "chain": [
                {
                  "from-server": "A",
                  "to-server": "B",
                  "method": "GET",
                  "path": "/b/resources",
                  "timestamp": "2025-02-10T12:34:56Z"
                },
                {
                  "from-server": "B",
                  "to-server": "C",
                  "method": "GET",
                  "path": "/c/resources",
                  "timestamp": "2025-02-10T12:35:00Z"
                }
              ]
            }
            """;

        PowsyblWsProblemDetail problem = OBJECT_MAPPER.readValue(json, PowsyblWsProblemDetail.class);

        assertEquals(403, problem.getStatus());
        assertEquals("Forbidden", problem.getTitle());
        assertEquals("Access denied", problem.getDetail());
        assertEquals("C", problem.getServer());
        assertEquals("PERMISSION_DENIED", problem.getBusinessErrorCode());
        assertEquals("2025-02-10T12:35:00Z", problem.getTimestamp().toString());
        assertEquals("/c/resources", problem.getPath());
        assertEquals("cid-77", problem.getTraceId());
        assertThat(problem.getChain()).hasSize(2);
        ChainEntry first = problem.getChain().get(0);
        ChainEntry second = problem.getChain().get(1);

        assertEquals("A", first.fromServer());
        assertEquals("B", first.toServer());
        assertEquals("GET", first.method());
        assertEquals("/b/resources", first.path());
        assertEquals(Instant.parse("2025-02-10T12:34:56Z"), first.timestamp());

        assertEquals("B", second.fromServer());
        assertEquals("C", second.toServer());
        assertEquals("GET", second.method());
        assertEquals("/c/resources", second.path());
        assertEquals(Instant.parse("2025-02-10T12:35:00Z"), second.timestamp());
    }
}
