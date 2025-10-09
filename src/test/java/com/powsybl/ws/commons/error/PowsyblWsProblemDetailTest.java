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
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail.HttpMethodValue;
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
        assertThat(node.get("server").asText()).isEqualTo("directory-server");
        assertThat(node.get("businessErrorCode").asText()).isEqualTo("directory.ERROR");
        assertThat(node.get("detail").asText()).isEqualTo("invalid payload");
        assertThat(node.get("timestamp").asText()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(node.get("path").asText()).isEqualTo("/directory-server/api");
        assertThat(node.get("traceId").asText()).isEqualTo("trace-1");
        assertThat(node.get("chain")).isNotNull();
        assertThat(node.get("chain")).isEmpty();
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

        assertThat(copy.chainEntries()).hasSize(2);
        ChainEntry first = copy.chainEntries().get(0);
        ChainEntry second = copy.chainEntries().get(1);
        assertThat(first.fromServer()).isEqualTo(new ServerName("a-server"));
        assertThat(first.toServer()).isEqualTo(new ServerName("b-server"));
        assertThat(first.method()).isEqualTo(HttpMethodValue.of("GET"));
        assertThat(first.path()).isEqualTo(PowsyblWsProblemDetail.ErrorPath.of("/b/resources"));
        assertThat(second.fromServer()).isEqualTo(new ServerName("b-server"));
        assertThat(second.toServer()).isEqualTo(new ServerName("c-server"));
        assertThat(second.method()).isEqualTo(HttpMethodValue.of("GET"));
        assertThat(second.path()).isEqualTo(PowsyblWsProblemDetail.ErrorPath.of("/c/resources"));
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

        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getTitle()).isEqualTo("Forbidden");
        assertThat(problem.getDetail()).isEqualTo("Access denied");
        assertThat(problem.getServer()).isEqualTo(new ServerName("C"));
        assertThat(problem.getBusinessErrorCode()).isEqualTo(new PowsyblWsProblemDetail.BusinessErrorCode("PERMISSION_DENIED"));
        assertThat(problem.timestamp()).contains(Instant.parse("2025-02-10T12:35:00Z"));
        assertThat(problem.path()).map(PowsyblWsProblemDetail.ErrorPath::value).contains("/c/resources");
        assertThat(problem.traceId()).map(PowsyblWsProblemDetail.TraceId::value).contains("cid-77");
        assertThat(problem.chainEntries()).hasSize(2);
        ChainEntry first = problem.chainEntries().get(0);
        ChainEntry second = problem.chainEntries().get(1);

        assertThat(first.fromServer()).isEqualTo(new ServerName("A"));
        assertThat(first.toServer()).isEqualTo(new ServerName("B"));
        assertThat(first.method()).isEqualTo(HttpMethodValue.of("GET"));
        assertThat(first.path()).isEqualTo(PowsyblWsProblemDetail.ErrorPath.of("/b/resources"));
        assertThat(first.timestamp()).isEqualTo(Instant.parse("2025-02-10T12:34:56Z"));

        assertThat(second.fromServer()).isEqualTo(new ServerName("B"));
        assertThat(second.toServer()).isEqualTo(new ServerName("C"));
        assertThat(second.method()).isEqualTo(HttpMethodValue.of("GET"));
        assertThat(second.path()).isEqualTo(PowsyblWsProblemDetail.ErrorPath.of("/c/resources"));
        assertThat(second.timestamp()).isEqualTo(Instant.parse("2025-02-10T12:35:00Z"));
    }
}
