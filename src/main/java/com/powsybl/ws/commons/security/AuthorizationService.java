/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security;

import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
public class AuthorizationService {

    private static final String PARAM_ACCESS_TYPE = "accessType";
    private static final String PARAM_IDS = "ids";
    private static final String PARAM_TARGET_DIRECTORY_UUID = "targetDirectoryUuid";
    private static final String PARAM_RECURSIVE_CHECK = "recursiveCheck";

    private final RestClient restClient;

    public AuthorizationService(RestClient.Builder restClientBuilder, String directoryServerBaseUri, String elementsAuthorizedPath) {
        this.restClient = restClientBuilder
                .baseUrl(directoryServerBaseUri + elementsAuthorizedPath)
                .build();
    }

    public boolean canRead(UUID elementUuid) {
        return canRead(List.of(elementUuid));
    }

    public boolean canRead(List<UUID> elementUuids) {
        checkPermission(elementUuids, null, PermissionType.READ, false);
        return true;
    }

    public boolean canWrite(UUID elementUuid) {
        return canWrite(List.of(elementUuid));
    }

    public boolean canWrite(List<UUID> elementUuids) {
        checkPermission(elementUuids, null, PermissionType.WRITE, false);
        return true;
    }

    public boolean canDuplicateTo(UUID elementUuid, UUID targetDirectoryUuid) {
        return canDuplicateTo(List.of(elementUuid), targetDirectoryUuid != null ? targetDirectoryUuid : elementUuid);
    }

    public boolean canDuplicateTo(List<UUID> elementUuids, UUID targetDirectoryUuid) {
        return canRead(elementUuids) && canWrite(targetDirectoryUuid);
    }

    public boolean canDelete(UUID elementUuid) {
        return canDelete(List.of(elementUuid));
    }

    public boolean canDelete(List<UUID> elementUuids) {
        checkPermission(elementUuids, null, PermissionType.WRITE, true);
        return true;
    }

    public boolean canMoveTo(List<UUID> elementUuids, UUID targetDirectoryUuid) {
        checkPermission(elementUuids, targetDirectoryUuid, PermissionType.WRITE, true);
        return true;
    }

    public boolean canManage(UUID elementUuid) {
        checkPermission(List.of(elementUuid), null, PermissionType.MANAGE, false);
        return true;
    }

    private void checkPermission(List<UUID> elementUuids, UUID targetDirectoryUuid, PermissionType permissionType, boolean recursiveCheck) {
        String ids = elementUuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .queryParam(PARAM_ACCESS_TYPE, permissionType)
                            .queryParam(PARAM_IDS, ids)
                            .queryParam(PARAM_RECURSIVE_CHECK, recursiveCheck);

                    if (targetDirectoryUuid != null) {
                        uriBuilder.queryParam(PARAM_TARGET_DIRECTORY_UUID, targetDirectoryUuid);
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .toBodilessEntity();
    }
}
