/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.springboot;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;

import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.HEADER_ROLES;
import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.HEADER_USER_ID;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
public class OpenApiCustomization implements OpenApiCustomizer {
    @Override
    public void customise(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        components.addSecuritySchemes(HEADER_USER_ID, apiKeyHeader(HEADER_USER_ID));
        components.addSecuritySchemes(HEADER_ROLES, apiKeyHeader(HEADER_ROLES));
        openApi.addSecurityItem(new SecurityRequirement()
                .addList(HEADER_USER_ID)
                .addList(HEADER_ROLES));
    }

    private static SecurityScheme apiKeyHeader(String name) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(name);
    }
}
