/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class BaseRestExceptionHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private TestRestExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestRestExceptionHandler(() -> "test-server");
    }

    @Test
    void handleDomainExceptionWithoutRemoteError() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/path");
        TestException exception = new TestException(TestBusinessErrorCode.LOCAL_FAILURE, "Local failure");

        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleDomainException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        PowsyblWsProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertEquals("test.local", body.getBusinessErrorCode());
        assertEquals("test-server", body.getServer());
        assertEquals("Local failure", body.getDetail());
        assertEquals("/test/path", body.getPath());
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getChain()).isEmpty();
    }

    @Test
    void handleRemoteExceptionWithValidPayloadWithChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/remote/call");
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.SERVICE_UNAVAILABLE)
            .server("downstream")
            .businessErrorCode("remote.failure")
            .detail("Remote chain failure")
            .path("/remote/service")
            .build();

        byte[] body = OBJECT_MAPPER.writeValueAsBytes(remote);
        HttpClientErrorException exception = HttpClientErrorException.create(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service unavailable",
            null,
            body,
            StandardCharsets.UTF_8
        );

        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleRemoteException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        PowsyblWsProblemDetail problem = response.getBody();
        assertThat(problem).isNotNull();
        assertEquals("remote.failure", problem.getBusinessErrorCode());
        assertThat(problem.getChain()).hasSize(1);
        assertEquals("downstream", problem.getChain().getFirst().toServer());
    }

    @Test
    void handleAllExceptionsUsesReasonPhraseWhenMessageMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/generic/error");
        Exception exception = new Exception("coucou");

        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleAllExceptions(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        PowsyblWsProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("coucou");
        assertThat(body.getBusinessErrorCode()).isNull();
    }

    @Test
    void handleAllExceptionsWithAnErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/generic/error");
        Exception exception = new Exception("root cause");
        Exception errorResponse = new ErrorResponseException(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "probably a bad request"), exception);

        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleAllExceptions(errorResponse, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        PowsyblWsProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getServer()).isEqualTo("test-server");
        assertThat(body.getPath()).isEqualTo("/generic/error");
        assertThat(body.getDetail()).isEqualTo("probably a bad request");
        assertThat(body.getBusinessErrorCode()).isNull();
        assertThat(body.getChain()).hasSize(1);
        var chainElement = body.getChain().getFirst();
        assertThat(chainElement.fromServer()).isEqualTo("test-server");
        assertThat(chainElement.toServer()).isEqualTo("test-server");
        assertThat(chainElement.method()).isEqualTo("PUT");
        assertThat(chainElement.path()).isEqualTo("/generic/error");
    }

    private enum TestBusinessErrorCode implements BusinessErrorCode {
        LOCAL_FAILURE("test.local"),
        REMOTE_FAILURE("test.remote");

        private final String value;

        TestBusinessErrorCode(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private static final class TestException extends AbstractBusinessException {

        private final TestBusinessErrorCode errorCode;

        private TestException(@NonNull TestBusinessErrorCode errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        @Override
        public @NonNull BusinessErrorCode getBusinessErrorCode() {
            return errorCode;
        }
    }

    private static final class TestRestExceptionHandler
        extends AbstractBaseRestExceptionHandler<TestException, TestBusinessErrorCode> {

        public TestRestExceptionHandler(ServerNameProvider serverNameProvider) {
            super(serverNameProvider);
        }

        @Override
        protected @NonNull TestBusinessErrorCode getBusinessCode(TestException ex) {
            return (TestBusinessErrorCode) ex.getBusinessErrorCode();
        }

        @Override
        protected HttpStatus mapStatus(TestBusinessErrorCode code) {
            return switch (code) {
                case LOCAL_FAILURE -> HttpStatus.BAD_REQUEST;
                case REMOTE_FAILURE -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
        }
    }
}
