package com.powsybl.ws.commons;

import com.powsybl.ws.commons.springboot.PowsyblWsCommonAutoConfiguration;
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

@SuppressWarnings("Convert2MethodRef")
@DisplayNameGeneration(DisplayNameGenerator.Simple.class)
class SpringBootAutoConfigurationTomcatTest implements WithAssertions {
    /*
     * We can't test if spring.factories or org.springframework.boot.autoconfigure.AutoConfiguration.imports
     * is detected because we aren't in a JAR during our tests...
     * so we limit tests to the configuration of the contexts.
     */
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootApplicationForTest.class, PowsyblWsCommonAutoConfiguration.class));

    @Test
    void testNormally() {
        this.contextRunner.run(context -> assertIsWebAppWithTomcatConfigured(context));
    }

    @Test
    void testWhenPropertyEnableIsInvalid() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.common.tomcat.encoded-solidus-handling=foo")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void testWhenPropertyEnableIsTrue() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.common.tomcat.encoded-solidus-handling=true")
            .run(context -> assertIsWebAppWithTomcatConfigured(context));
    }

    private void assertIsWebAppWithTomcatConfigured(@NonNull final AssertableWebApplicationContext context) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class); //auto-configuration from spring-boot
        assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class); //@Configuration class
        assertThat(context).hasBean("powsyblCustomizeTomcatConnector"); //@Bean created
    }

    @Test
    void testWhenPropertyEnableIsFalse() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.common.tomcat.encoded-solidus-handling=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class);
                assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
                assertThat(context).doesNotHaveBean("powsyblCustomizeTomcatConnector");
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
                assertThat(context).doesNotHaveBean("powsyblCustomizeTomcatConnector");
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
                assertThat(context).doesNotHaveBean("powsyblCustomizeTomcatConnector");
            });
    }
}
