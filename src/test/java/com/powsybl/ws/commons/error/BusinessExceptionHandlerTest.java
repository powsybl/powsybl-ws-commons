/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class BusinessExceptionHandlerTest {

    private TestRestExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestRestExceptionHandler(() -> "test-server");
    }

    @Test
    void handleDomainExceptionWithoutRemoteError() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/path");
        TestException exception = new TestException(
            TestBusinessErrorCode.LOCAL_FAILURE,
            "Local failure",
            Map.of("fields", List.of("A", "B"))
        );
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
        assertThat(body.getBusinessErrorValues()).containsEntry("fields", List.of("A", "B"));
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
        private final Map<String, Object> businessErrorValues;

        private TestException(@NonNull TestBusinessErrorCode errorCode, String message, Map<String, Object> businessErrorValues) {
            super(message);
            this.errorCode = errorCode;
            this.businessErrorValues = businessErrorValues;
        }

        @Override
        public @NonNull TestBusinessErrorCode getBusinessErrorCode() {
            return errorCode;
        }

        @Override
        public @NonNull Map<String, Object> getBusinessErrorValues() {
            return businessErrorValues;
        }
    }

    private static final class TestRestExceptionHandler
        extends AbstractBusinessExceptionHandler<TestException, TestBusinessErrorCode> {

        public TestRestExceptionHandler(ServerNameProvider serverNameProvider) {
            super(serverNameProvider);
        }

        @Override
        protected @NonNull TestBusinessErrorCode getBusinessCode(TestException ex) {
            return ex.getBusinessErrorCode();
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
