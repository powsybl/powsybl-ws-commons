/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
class AuthorizationServiceTest {

    private static final String BASE_URI = "http://directory-server/";
    private static final UUID ELEMENT_UUID = UUID.randomUUID();
    private static final UUID ELEMENT_UUID_2 = UUID.randomUUID();
    private static final UUID TARGET_DIRECTORY_UUID = UUID.randomUUID();

    //@Autowired
    private MockRestServiceServer directoryServer; // rename as server
    private AuthorizationService authorizationService;

    @BeforeEach // to delete
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        directoryServer = MockRestServiceServer.bindTo(restClientBuilder).build(); // TODO remove
        authorizationService = new AuthorizationService(restClientBuilder, BASE_URI); // à revoir ?
    }

    @AfterEach
    void verifyRequests() {
        directoryServer.verify();
    }

    @Test
    void shouldAllowReadElement() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.READ, false, HttpStatus.OK);

        assertThat(authorizationService.canRead(ELEMENT_UUID)).isTrue();
    }

    @Test
    void shouldPropagateExceptionWhenReadElementPermissionDenied() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.READ, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canRead(ELEMENT_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowReadElements() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.READ, false, HttpStatus.OK);

        assertThat(authorizationService.canRead(elementUuids)).isTrue();
    }

    @Test
    void shouldPropagateExceptionWhenReadElementsPermissionDenied() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.READ, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canRead(elementUuids))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowWriteElement() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.WRITE, false, HttpStatus.OK);

        assertThat(authorizationService.canWrite(ELEMENT_UUID)).isTrue();
    }

    @Test
    void shouldPropagateExceptionWhenWriteElementPermissionDenied() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.WRITE, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canWrite(ELEMENT_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowWriteElements() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.WRITE, false, HttpStatus.OK);

        assertThat(authorizationService.canWrite(elementUuids)).isTrue();
    }

    @Test
    void shouldPropagateExceptionWhenWriteElementsPermissionDenied() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.WRITE, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canWrite(elementUuids))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowDuplicateElementTo() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.READ, false, HttpStatus.OK);
        expectPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE, false, HttpStatus.OK);

        assertThat(authorizationService.canDuplicateTo(ELEMENT_UUID, TARGET_DIRECTORY_UUID)).isTrue();
    }

    @Test
    void shouldAllowDuplicateElementToWhenTargetDirectoryUuidIsNull() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.READ, false, HttpStatus.OK);
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.WRITE, false, HttpStatus.OK);

        assertThat(authorizationService.canDuplicateTo(ELEMENT_UUID, null)).isTrue();
    }

    @Test
    void shouldPropagateAccessDeniedExceptionWhenDuplicatingElementWithoutReadPermission() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.READ, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canDuplicateTo(ELEMENT_UUID, TARGET_DIRECTORY_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldPropagateAccessDeniedExceptionWhenDuplicatingElementWithoutWritePermissionOnTargetDirectory() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.READ, false, HttpStatus.OK);
        expectPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canDuplicateTo(ELEMENT_UUID, TARGET_DIRECTORY_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowDuplicateElementsTo() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.READ, false, HttpStatus.OK);
        expectPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE, false, HttpStatus.OK);

        assertThat(authorizationService.canDuplicateTo(elementUuids, TARGET_DIRECTORY_UUID)).isTrue();
    }

    @Test
    void shouldPropagateAccessDeniedExceptionWhenDuplicatingElementsWithoutReadPermission() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.READ, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canDuplicateTo(elementUuids, TARGET_DIRECTORY_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldPropagateAccessDeniedExceptionWhenDuplicatingElementsWithoutWritePermissionOnTargetDirectory() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.READ, false, HttpStatus.OK);
        expectPermission(List.of(TARGET_DIRECTORY_UUID), null, PermissionType.WRITE, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canDuplicateTo(elementUuids, TARGET_DIRECTORY_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowDeleteElement() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.WRITE, true, HttpStatus.OK);

        assertThat(authorizationService.canDelete(ELEMENT_UUID)).isTrue();
    }

    @Test
    void shouldPropagateExceptionWhenDeleteElementPermissionDenied() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.WRITE, true, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canDelete(ELEMENT_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowDeleteElements() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.WRITE, true, HttpStatus.OK);

        assertThat(authorizationService.canDelete(elementUuids)).isTrue();
    }

    @Test
    void shouldPropagateExceptionWhenDeleteElementsPermissionDenied() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, null, PermissionType.WRITE, true, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canDelete(elementUuids))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowMoveTo() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, TARGET_DIRECTORY_UUID, PermissionType.WRITE, true, HttpStatus.OK);

        assertThat(authorizationService.canMoveTo(elementUuids, TARGET_DIRECTORY_UUID)).isTrue();
    }

    @Test
    void shouldPropagateExceptionWhenMoveElementPermissionDenied() {
        List<UUID> elementUuids = List.of(ELEMENT_UUID, ELEMENT_UUID_2);
        expectPermission(elementUuids, TARGET_DIRECTORY_UUID, PermissionType.WRITE, true, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canMoveTo(elementUuids, TARGET_DIRECTORY_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void shouldAllowManage() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.MANAGE, false, HttpStatus.OK);

        assertThat(authorizationService.canManage(ELEMENT_UUID)).isTrue();
    }

    @Test
    void shouldPropagateExceptionWhenManageElementPermissionDenied() {
        expectPermission(List.of(ELEMENT_UUID), null, PermissionType.MANAGE, false, HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.canManage(ELEMENT_UUID))
                .isInstanceOf(RestClientResponseException.class);
    }

    private void expectPermission(List<UUID> elementUuids, UUID targetDirectoryUuid, PermissionType permissionType,
                                  boolean recursiveCheck, HttpStatus responseStatus) {
        String ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        directoryServer.expect(mockRequest -> {
            assertThat(mockRequest.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(mockRequest.getURI())
                    .hasScheme("http")
                    .hasHost("directory-server")
                    .hasPath("/v1/elements/authorized");

            var queryParams = UriComponentsBuilder.fromUri(mockRequest.getURI()).build().getQueryParams();
            assertThat(queryParams.get("accessType")).containsExactly(permissionType.name());
            assertThat(queryParams.get("ids")).containsExactly(ids);
            assertThat(queryParams.get("recursiveCheck")).containsExactly(Boolean.toString(recursiveCheck));

            if (targetDirectoryUuid != null) {
                assertThat(queryParams.get("targetDirectoryUuid")).containsExactly(targetDirectoryUuid.toString());
            } else {
                assertThat(queryParams).doesNotContainKey("targetDirectoryUuid");
            }
        }).andRespond(withStatus(responseStatus));
    }
}
