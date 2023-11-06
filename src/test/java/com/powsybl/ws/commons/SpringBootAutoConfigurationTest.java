package com.powsybl.ws.commons;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
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
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

@DisplayNameGeneration(DisplayNameGenerator.Simple.class)
class SpringBootAutoConfigurationTest implements WithAssertions {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootAutoConfigurationTest.class));

    @Test
    void testWhenNotWebApplication() {
        final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SpringBootAutoConfigurationTest.class));
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(TomcatAutoCustomization.class);
            assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class);
            assertThat(context).doesNotHaveBean(ConfigurableTomcatWebServerFactory.class);
        });
    }

    @Test
    void testWhenIsWebApplication() {
        this.contextRunner
            //.withUserConfiguration(MyConfiguration.class)
            .run(context -> {
                assertIsWebAppWithTomcatConfigured(context);
            });
    }

    @Test
    void testWhenIsWebApplicationWithPropertySkipFalse() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.skip-init=false")
            .run(context -> {
                assertIsWebAppWithTomcatConfigured(context);
            });
    }

    @Test
    void testWhenIsWebApplicationWithPropertySkipInvalid() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.skip-init=foo")
            .run(context -> {
                assertIsWebAppWithTomcatConfigured(context);
            });
    }

    @Test
    void autoConfigurationIsIgnoredIfPropertySkipIsSet() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.skip-init=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(TomcatAutoCustomization.class);
                assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class);
                assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class);
            });
    }

    @Test
    void autoConfigurationIsIgnoredIfClassNotPresent() {
        this.contextRunner
            .withClassLoader(new FilteredClassLoader(TomcatAutoCustomization.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(TomcatAutoCustomization.class);
                assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class);
            });
    }

    private /*static*/ void assertIsWebAppWithTomcatConfigured(@NonNull final AssertableWebApplicationContext context) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(TomcatAutoCustomization.class); //@Configuration class
        assertThat(context).getBeans(WebServerFactoryCustomizer.class).isNotEmpty(); //@Bean created
        assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class); //auto-configuration from spring-boot
        assertThat(context).getBean(Connector.class).as("Tomcat Catalina connector")
                .hasFieldOrPropertyWithValue("encodedSolidusHandling", EncodedSolidusHandling.PASS_THROUGH.getValue());
    }
}
