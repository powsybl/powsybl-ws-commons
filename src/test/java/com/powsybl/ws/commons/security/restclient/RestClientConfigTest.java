/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security.restclient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.HEADER_ROLES;
import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.HEADER_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Achour Berrahma {@literal <achour.berrahma at rte-france.com>}
 */
@ExtendWith(SpringExtension.class)
//NOTE: this surprises given the name AutoConfigureWebClient, but this is not actually web clients,
// it is builders for webclients, and it's not just webflux webclient,
// it's also resttemplatebuilder that we need.
// And other builders are also registered but without consequences, they're just unused.
// In the future springboot 4.0.0 is supposed to have changed the name to be less surprising
class RestClientConfigTest {

    private RestClient restClient;

    private MockRestServiceServer mockServer;
    private static final String TEST_ROLES = "ADMIN|USER";
    private static final String TEST_USER_ID = "user";
    private static final String TEST_ENDPOINT = "http://test-service/";

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        restClientBuilder.requestInterceptor(new HeaderForwardingInterceptor());
        restClient = restClientBuilder.build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = TEST_USER_ID, authorities = TEST_ROLES)
    void testRoleAndUserIdHeaderIsPropagated() {
        mockServer.expect(request -> {
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getURI()).hasToString(TEST_ENDPOINT);
            assertThat(request.getHeaders().getFirst(HEADER_ROLES)).isEqualTo(TEST_ROLES);
            assertThat(request.getHeaders().getFirst(HEADER_USER_ID)).isEqualTo(TEST_USER_ID);
        }).andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"success\"}", MediaType.APPLICATION_JSON));

        restClient.get()
                .uri(TEST_ENDPOINT)
                .retrieve()
                .body(String.class);

        mockServer.verify();
    }

    @Test
    void testNoRoleAndUserIdHeaderPropagationWhenNotPresent() {
        mockServer.expect(request -> {
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getURI()).hasToString(TEST_ENDPOINT);
            assertThat(request.getHeaders().get(HEADER_ROLES)).isNull();
            assertThat(request.getHeaders().get(HEADER_USER_ID)).isNull();
        }).andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"success\"}", MediaType.APPLICATION_JSON));

        restClient.get()
                .uri(TEST_ENDPOINT)
                .retrieve()
                .body(String.class);

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = TEST_USER_ID, roles = {})
    void testEmptyRoleHeaderNotPropagated() {
        mockServer.expect(request -> {
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getURI()).hasToString(TEST_ENDPOINT);
            assertThat(request.getHeaders().get(HEADER_ROLES)).isNull();
        }).andRespond(MockRestResponseCreators.withSuccess("{\"result\":\"success\"}", MediaType.APPLICATION_JSON));

        restClient.get()
                .uri(TEST_ENDPOINT)
                .retrieve()
                .body(String.class);

        mockServer.verify();
    }
}
