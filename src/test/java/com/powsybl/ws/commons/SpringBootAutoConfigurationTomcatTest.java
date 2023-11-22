/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

import com.powsybl.ws.commons.springboot.PowsyblWsCommonAutoConfiguration;
import com.powsybl.ws.commons.springboot.TomcatCustomization;
import com.powsybl.ws_common_spring_test.SpringBootApplicationForTest;
import org.apache.catalina.startup.Tomcat;
import org.assertj.core.api.WithAssertions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;

/**
 * We test the Tomcat configuration
 */
@SuppressWarnings("Convert2MethodRef")
@DisplayNameGeneration(DisplayNameGenerator.Simple.class)
class SpringBootAutoConfigurationTomcatTest implements WithAssertions {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootApplicationForTest.class, PowsyblWsCommonAutoConfiguration.class));

    @Test
    void testNormally() {
        this.contextRunner.run(context -> assertIsWebAppWithTomcatConfigured(context));
    }

    @Test
    void testWhenPropertyEnableIsInvalid() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.autoconfigure.tomcat-customize.enable=foo")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void testWhenPropertyEnableIsTrue() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.autoconfigure.tomcat-customize.enable=true")
            .run(context -> assertIsWebAppWithTomcatConfigured(context));
    }

    @Test
    void testWhenPropertyEncodedSolidusHandlingIsInvalid() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.autoconfigure.tomcat-customize.encoded-solidus-handling=foo")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void testWhenPropertyEncodedSolidusHandlingIsTrue() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.autoconfigure.tomcat-customize.encoded-solidus-handling=true")
            .run(context -> assertIsWebAppWithTomcatConfigured(context));
    }

    private void assertIsWebAppWithTomcatConfigured(@NonNull final AssertableWebApplicationContext context) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class); //auto-configuration from spring-boot
        assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class); //@Configuration class
        assertThat(context).hasBean("powsyblTomcatConnectorCustomizer"); //@Bean created
    }

    @Test
    void testWhenPropertyEnableIsFalse() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.autoconfigure.tomcat-customize.enable=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class);
                assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
                assertThat(context).doesNotHaveBean("powsyblTomcatConnectorCustomizer");
                assertThat(context).doesNotHaveBean(TomcatCustomization.class);
                assertThat(context).doesNotHaveBean(TomcatConnectorCustomizer.class);
            });
    }

    @Test
    void testWhenTomcatNotPresent() {
        this.contextRunner
            .withClassLoader(new FilteredClassLoader(Tomcat.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ConfigurableTomcatWebServerFactory.class);
                assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
                assertThat(context).doesNotHaveBean("powsyblTomcatConnectorCustomizer");
                assertThat(context).doesNotHaveBean(TomcatCustomization.class);
                assertThat(context).doesNotHaveBean(TomcatConnectorCustomizer.class);
            });
    }

    @Test
    void testWhenIsNotWebApplication() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootApplicationForTest.class, PowsyblWsCommonAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ConfigurableTomcatWebServerFactory.class);
                assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
                assertThat(context).doesNotHaveBean("powsyblTomcatConnectorCustomizer");
                assertThat(context).doesNotHaveBean(TomcatCustomization.class);
                assertThat(context).doesNotHaveBean(TomcatConnectorCustomizer.class);
            });
    }
}
