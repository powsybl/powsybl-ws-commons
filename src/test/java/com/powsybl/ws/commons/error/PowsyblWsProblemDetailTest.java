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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.slf4j.MDC;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.HttpStatusCodeException;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@ExtendWith(MockitoExtension.class)
class PowsyblWsProblemDetailTest {

    private static final String SERVER_NAME = "my-server";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static MDCAdapter originalMdcAdapter;
    private static Field mdcAdapterField;
    private static boolean replacedMdcAdapter;

    @BeforeAll
    static void ensureMdcAdapter() throws Exception {
        mdcAdapterField = findMdcAdapterField();
        originalMdcAdapter = (MDCAdapter) mdcAdapterField.get(null);
        if (originalMdcAdapter instanceof NOPMDCAdapter) {
            mdcAdapterField.set(null, new ThreadLocalMdcAdapter());
            replacedMdcAdapter = true;
        }
    }

    @AfterAll
    static void restoreMdcAdapter() throws Exception {
        if (replacedMdcAdapter) {
            mdcAdapterField.set(null, originalMdcAdapter);
        }
    }

    private static Field findMdcAdapterField() throws Exception {
        for (Field field : MDC.class.getDeclaredFields()) {
            if (MDCAdapter.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("No MDCAdapter field found on MDC");
    }

    @Test
    void serializesTypedProperties() throws Exception {
        PowsyblWsProblemDetail problem = PowsyblWsProblemDetail.builder(HttpStatus.BAD_REQUEST)
            .server("directory-server")
            .businessErrorCode("directory.ERROR")
            .detail("invalid payload")
            .path("/directory-server/api")
            .businessErrorValues(Map.of("fields", List.of("A", "B")))
            .build();

        String json = OBJECT_MAPPER.writeValueAsString(problem);
        JsonNode node = OBJECT_MAPPER.readTree(json);
        assertEquals("directory-server", node.get("server").asText());
        assertEquals("directory.ERROR", node.get("businessErrorCode").asText());
        assertEquals("invalid payload", node.get("detail").asText());
        assertEquals("/directory-server/api", node.get("path").asText());
        assertNotNull(node.get("chain"));
        assertNotNull(node.get("businessErrorValues"));
        assertEquals(2, node.get("businessErrorValues").get("fields").size());
        assertThat(node.get("businessErrorValues").get("fields")).isNotNull();
    }

    @Test
    void builderAddsTraceIdFromMdc() {
        MDC.put("traceId", "unique-trace-id");
        try {
            PowsyblWsProblemDetail problem = PowsyblWsProblemDetail.builder(HttpStatus.BAD_REQUEST)
                .server("directory-server")
                .businessErrorCode("directory.ERROR")
                .detail("invalid payload")
                .path("/directory-server/api")
                .build();

            assertEquals("unique-trace-id", problem.getTraceId());
        } finally {
            MDC.remove("unique-trace-id");
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
              "businessErrorValues": {
                "fields": ["A", "B", "C"]
              },
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
        assertThat(problem.getBusinessErrorValues()).containsEntry("fields", List.of("A", "B", "C"));
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

    @Test
    void fromExceptionWithBusinessException(@Mock AbstractBusinessException businessException) {
        when(businessException.getBusinessErrorCode()).thenReturn(() -> "ERR123");

        Map<String, Object> values = Map.of(
            "k1", "v1",
            "k2", 42
        );
        when(businessException.getBusinessErrorValues()).thenReturn(values);
        when(businessException.getMessage()).thenReturn("business failed");

        var detail = PowsyblWsProblemDetail.fromException(businessException, SERVER_NAME);

        assertThat(detail.getServer()).isEqualTo(SERVER_NAME);
        assertThat(detail.getBusinessErrorCode()).isEqualTo("ERR123");
        assertThat(detail.getBusinessErrorValues())
            .containsExactlyInAnyOrderEntriesOf(values);
        assertThat(detail.getDetail()).isEqualTo("business failed");
        assertThat(detail.getChain()).isEmpty(); // no wrap() for business errors
    }

    @Test
    void fromExceptionWithHttpStatusCodeExceptionParsesBody() {
        String json = """
            {
                "server": "other-server",
                "status": 403,
                "detail": "parsed-body"
            }
            """;
        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);

        HttpStatusCodeException httpEx = createHttpStatusException(HttpStatus.UNAUTHORIZED, "erreur", bodyBytes);

        var result = PowsyblWsProblemDetail.fromException(httpEx, SERVER_NAME);

        assertThat(result.getServer()).isEqualTo("other-server");
        assertThat(result.getStatus()).isEqualTo(403);
        assertThat(result.getDetail()).isEqualTo("parsed-body");
        assertThat(result.getChain()).isNotEmpty();
        assertThat(result.getChain().getFirst().fromServer()).isEqualTo(SERVER_NAME);
        assertThat(result.getChain().getFirst().toServer()).isEqualTo("other-server");
    }

    @Test
    void fromExceptionWithHttpStatusCodeExceptionFallbackOnParsingFailure() {
        byte[] invalid = "{bad json".getBytes(StandardCharsets.UTF_8);

        HttpStatusCodeException httpEx = createHttpStatusException(HttpStatus.BAD_REQUEST, "erreur", invalid);

        var result = PowsyblWsProblemDetail.fromException(httpEx, SERVER_NAME);

        assertThat(result.getDetail()).isEqualTo("400 erreur");
        assertThat(result.getChain()).isNotEmpty();
        assertThat(result.getChain().getFirst().fromServer()).isEqualTo(SERVER_NAME);
        assertThat(result.getChain().getFirst().toServer()).isEqualTo(SERVER_NAME);
    }

    @Test
    void fromExceptionWithErrorResponse() {
        PowsyblWsProblemDetail problemDetail = PowsyblWsProblemDetail.builder()
            .server(SERVER_NAME)
            .detail("from-body")
            .build();

        ErrorResponseException ex = new ErrorResponseException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            problemDetail,
            new RuntimeException("error")
        );

        var result = PowsyblWsProblemDetail.fromException(ex, SERVER_NAME);

        assertThat(result.getDetail()).isEqualTo("from-body");
        assertThat(result.getServer()).isEqualTo(SERVER_NAME);
        assertThat(result.getChain()).isNotEmpty();
        assertThat(result.getChain().getFirst().fromServer()).isEqualTo(SERVER_NAME);
        assertThat(result.getChain().getFirst().toServer()).isEqualTo(SERVER_NAME);
    }

    @Test
    void fromExceptionDefaultCase() {
        Exception ex = new Exception("default");

        var result = PowsyblWsProblemDetail.fromException(ex, SERVER_NAME);

        assertThat(result.getDetail()).isEqualTo("default");
        assertThat(result.getServer()).isEqualTo(SERVER_NAME);
        assertThat(result.getChain()).isEmpty();
    }

    private HttpStatusCodeException createHttpStatusException(HttpStatus status, String message, byte[] body) {
        return new HttpStatusCodeException(
            status,
            message,
            body,
            StandardCharsets.UTF_8
        ) {
            @Override
            public byte[] getResponseBodyAsByteArray() {
                return body;
            }
        };
    }

    /**
     * IDE execute these tests with SLF4J's no-op implementation, which comes without an MDCAdapter.
     * This minimal ThreadLocal substitute keeps MDC-based traceId lookups working without pulling a
     * heavyweight logging backend into test scope.
     */
    private static final class ThreadLocalMdcAdapter implements MDCAdapter {

        private final ThreadLocal<Map<String, String>> context = ThreadLocal.withInitial(HashMap::new);
        private final ThreadLocal<Map<String, Deque<String>>> stacks = ThreadLocal.withInitial(HashMap::new);

        @Override
        public void put(String key, String val) {
            if (val == null) {
                remove(key);
            } else {
                context.get().put(key, val);
            }
        }

        @Override
        public String get(String key) {
            return context.get().get(key);
        }

        @Override
        public void remove(String key) {
            context.get().remove(key);
        }

        @Override
        public void clear() {
            context.get().clear();
            stacks.get().clear();
        }

        @Override
        public Map<String, String> getCopyOfContextMap() {
            Map<String, String> map = context.get();
            return map.isEmpty() ? null : new HashMap<>(map);
        }

        @Override
        public void setContextMap(Map<String, String> contextMap) {
            Map<String, String> map = context.get();
            map.clear();
            if (contextMap != null) {
                map.putAll(contextMap);
            }
        }

        @Override
        public void pushByKey(String key, String value) {
            if (value != null) {
                stacks.get().computeIfAbsent(key, ignored -> new ArrayDeque<>()).push(value);
            }
        }

        @Override
        public String popByKey(String key) {
            Deque<String> deque = stacks.get().get(key);
            if (deque == null) {
                return null;
            }
            String value = deque.poll();
            if (deque.isEmpty()) {
                stacks.get().remove(key);
            }
            return value;
        }

        @Override
        public void clearDequeByKey(String key) {
            stacks.get().remove(key);
        }

        @Override
        public Deque<String> getCopyOfDequeByKey(String key) {
            Deque<String> deque = stacks.get().get(key);
            return (deque == null || deque.isEmpty()) ? null : new ArrayDeque<>(deque);
        }
    }
}
