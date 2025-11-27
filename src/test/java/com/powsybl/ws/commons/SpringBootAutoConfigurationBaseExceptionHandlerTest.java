/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

import com.powsybl.ws.commons.springboot.PowsyblWsCommonAutoConfiguration;
import com.powsybl.ws_common_spring_test.SpringBootApplicationForTest;
import lombok.NonNull;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * We test the base exception handler configuration
 */
@SuppressWarnings("Convert2MethodRef")
@DisplayNameGeneration(DisplayNameGenerator.Simple.class)
class SpringBootAutoConfigurationBaseExceptionHandlerTest implements WithAssertions {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootApplicationForTest.class, PowsyblWsCommonAutoConfiguration.class));

    @Test
    void testNormally() {
        this.contextRunner.run(this::assertIsWebAppWithBaseExceptionHandlerConfigured);
    }

    @Test
    void testWhenPropertyEnableIsInvalid() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.autoconfigure.base-exception-handler.enable=foo")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void testWhenPropertyEnableIsTrue() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.autoconfigure.base-exception-handler.enable=true")
            .run(this::assertIsWebAppWithBaseExceptionHandlerConfigured);
    }

    private void assertIsWebAppWithBaseExceptionHandlerConfigured(@NonNull final AssertableWebApplicationContext context) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class); //@Configuration class
        assertThat(context).hasBean("powsyblBaseExceptionHandler"); //@Bean created
    }

    @Test
    void testWhenPropertyEnableIsFalse() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.autoconfigure.base-exception-handler.enable=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
                assertThat(context).doesNotHaveBean("powsyblBaseExceptionHandler");
            });
    }

    @Test
    void testWhenIsNotWebApplication() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootApplicationForTest.class, PowsyblWsCommonAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
                assertThat(context).doesNotHaveBean("powsyblBaseExceptionHandler");
            });
    }
}
