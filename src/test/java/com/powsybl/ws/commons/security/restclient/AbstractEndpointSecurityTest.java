/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.security.restclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */

/**
 * Base class for testing REST endpoint security across an application.
 * This test ensures that every endpoint exposed by the controllers in the configured packages is secured with a {@link PreAuthorize}
 * annotation. It helps prevent accidentally exposing unsecured endpoints when adding new controllers or endpoints.
 * To use this test, extend this class.
 */
@SpringBootTest
public abstract class AbstractEndpointSecurityTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    /**
     *  Returns the package names containing controllers whose endpoints must be secured with a {@link PreAuthorize} annotation
     *  @return the list of controller package names to check
     */
    protected abstract List<String> getControllerPackages();

    /**
     *  Returns the controllers to exclude from security annotation checks.
     *  For example, SupervisionController's endpoints are usually intentionnally not secured
     *  @return the list of controllers to exclude
     */
    protected abstract List<Class<?>> getExcludedControllers();

    @Test
    void allEndpointsMustHaveSecurityAnnotation() {
        List<String> unsecuredEndpoints = new ArrayList<>();

        mappings.getHandlerMethods().forEach((info, handler) -> {
            Method method = handler.getMethod();
            Class<?> controller = handler.getBeanType();
            if (!getControllerPackages().contains(controller.getPackageName())
                    || getExcludedControllers().contains(controller)) {
                return;
            }

            if (!hasSecurityAnnotation(method, controller)) {
                unsecuredEndpoints.add(
                        controller.getSimpleName() + "#" + method.getName()
                );
            }
        });

        assertThat(unsecuredEndpoints).isEmpty();
    }

    private boolean hasSecurityAnnotation(Method method, Class<?> controller) {
        return AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
    }
}
