/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.springboot;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.HEADER_ROLES;
import static com.powsybl.ws.commons.security.AuthorizationSystemConstants.HEADER_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
class OpenApiCustomizationTest {

    @Test
    void shouldCreateComponentsAndAddHeaderSecuritySchemes() {
        OpenAPI openApi = new OpenAPI();

        new OpenApiCustomization().customise(openApi);

        assertThat(openApi.getComponents()).isNotNull();
        assertApiKeyHeader(openApi.getComponents(), HEADER_USER_ID);
        assertApiKeyHeader(openApi.getComponents(), HEADER_ROLES);
        assertThat(openApi.getSecurity()).hasSize(1);
        assertThat(openApi.getSecurity().get(0))
                .containsKeys(HEADER_USER_ID, HEADER_ROLES);
    }

    @Test
    void shouldReuseExistingComponents() {
        Components components = new Components();
        OpenAPI openApi = new OpenAPI().components(components);

        new OpenApiCustomization().customise(openApi);

        assertThat(openApi.getComponents()).isSameAs(components);
        assertApiKeyHeader(components, HEADER_USER_ID);
        assertApiKeyHeader(components, HEADER_ROLES);
    }

    private static void assertApiKeyHeader(Components components, String headerName) {
        SecurityScheme securityScheme = components.getSecuritySchemes().get(headerName);
        assertThat(securityScheme).isNotNull();
        assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(securityScheme.getIn()).isEqualTo(SecurityScheme.In.HEADER);
        assertThat(securityScheme.getName()).isEqualTo(headerName);
    }
}
