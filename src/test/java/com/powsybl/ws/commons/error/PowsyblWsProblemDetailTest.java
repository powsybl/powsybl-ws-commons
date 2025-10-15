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
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        PowsyblWsProblemDetail problem = PowsyblWsProblemDetail.builder(HttpStatus.BAD_REQUEST)
            .title("Bad Request")
            .server("directory-server")
            .businessErrorCode("directory.ERROR")
            .detail("invalid payload")
            .timestamp(now)
            .path("/directory-server/api")
            .traceId("trace-1")
            .build();

        String json = OBJECT_MAPPER.writeValueAsString(problem);
        JsonNode node = OBJECT_MAPPER.readTree(json);
        assertEquals("directory-server", node.get("server").asText());
        assertEquals("directory.ERROR", node.get("businessErrorCode").asText());
        assertEquals("invalid payload", node.get("detail").asText());
        assertEquals("2025-01-01T00:00:00Z", node.get("timestamp").asText());
        assertEquals("/directory-server/api", node.get("path").asText());
        assertEquals("trace-1", node.get("traceId").asText());
        assertNotNull(node.get("chain"));
        assertNotNull(node.get("chain"));
    }

    @Test
    void builderFromKeepsExistingChainWithoutDuplicating() {
        Instant now = Instant.parse("2025-03-02T10:15:30Z");
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.FORBIDDEN)
            .server("c-server")
            .businessErrorCode("REMOTE_DENIED")
            .detail("upstream failure")
            .timestamp(now)
            .path("/c/resources")
            .build();

        PowsyblWsProblemDetail wrapped = PowsyblWsProblemDetail.builderFrom(remote)
            .appendChain("b-server", "GET", "/c/resources", HttpStatus.FORBIDDEN.value(), now)
            .build();

        PowsyblWsProblemDetail copy = PowsyblWsProblemDetail.builderFrom(wrapped)
            .appendChain("a-server", "GET", "/b/resources", HttpStatus.FORBIDDEN.value(), now)
            .build();

        assertThat(copy.getChain()).hasSize(2);
        ChainEntry first = copy.getChain().get(0);
        ChainEntry second = copy.getChain().get(1);
        assertEquals("a-server", first.getFromServer());
        assertEquals("b-server", first.getToServer());
        assertEquals("GET", first.getMethod());
        assertEquals("/b/resources", first.getPath());
        assertEquals("b-server", second.getFromServer());
        assertEquals("c-server", second.getToServer());
        assertEquals("GET", second.getMethod());
        assertEquals("/c/resources", second.getPath());
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
                  "status": 403,
                  "timestamp": "2025-02-10T12:34:56Z"
                },
                {
                  "from-server": "B",
                  "to-server": "C",
                  "method": "GET",
                  "path": "/c/resources",
                  "status": 403,
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

        assertEquals("A", first.getFromServer());
        assertEquals("B", first.getToServer());
        assertEquals("GET", first.getMethod());
        assertEquals("/b/resources", first.getPath());
        assertEquals(Instant.parse("2025-02-10T12:34:56Z"), first.getTimestamp());

        assertEquals("B", second.getFromServer());
        assertEquals("C", second.getToServer());
        assertEquals("GET", second.getMethod());
        assertEquals("/c/resources", second.getPath());
        assertEquals(Instant.parse("2025-02-10T12:35:00Z"), second.getTimestamp());
    }
}
