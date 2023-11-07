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
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.context.ApplicationContext;

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
        this.contextRunner
            //.withUserConfiguration(MyConfiguration.class)
            .run(context -> assertIsWebAppWithTomcatConfigured(context));
    }

    @Test
    void testWhenPropertyEnableIsInvalid() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.tomcat.encoded-solidus-handling=foo")
            .run(context -> assertIsWebAppWithTomcatConfigured(context));
    }

    @Test
    void testWhenPropertyEnableIsTrue() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.tomcat.encoded-solidus-handling=true")
            .run(context -> assertIsWebAppWithTomcatConfigured(context));
    }

    private /*static*/ void assertIsWebAppWithTomcatConfigured(@NonNull final AssertableWebApplicationContext context) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class); //auto-configuration from spring-boot
        assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class); //@Configuration class
        assertThat(context).hasBean("powsyblCustomizeTomcatFactory"); //@Bean created
        //assertThat(context).getBean(Connector.class).as("Tomcat Catalina connector")
        //        .hasFieldOrPropertyWithValue("encodedSolidusHandling", EncodedSolidusHandling.PASS_THROUGH.getValue());
    }

    @Test
    void testWhenPropertyEnableIsFalse() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.tomcat.encoded-solidus-handling=false")
            .run(context -> assertIsWebAppWithTomcatNotConfigured(context));
    }

    @Test
    void testWhenTomcatNotPresent() {
        this.contextRunner
            .withClassLoader(new FilteredClassLoader(Tomcat.class))
            //.run(context -> assertIsWebAppWithTomcatNotConfigured(context));
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ConfigurableTomcatWebServerFactory.class);
                assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
                assertThat(context).doesNotHaveBean("powsyblCustomizeTomcatFactory");
            });
    }

    @Test
    void testWhenIsNotWebApplication() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootApplicationForTest.class, PowsyblWsCommonAutoConfiguration.class))
            //.run(context -> assertIsWebAppWithTomcatNotConfigured(context));
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ConfigurableTomcatWebServerFactory.class);
                assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
                assertThat(context).doesNotHaveBean("powsyblCustomizeTomcatFactory");
            });
    }

    private /*static*/ void assertIsWebAppWithTomcatNotConfigured(@NonNull final ApplicationContextAssertProvider<? extends ApplicationContext> context) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(ConfigurableTomcatWebServerFactory.class);
        assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
        assertThat(context).doesNotHaveBean("powsyblCustomizeTomcatFactory");
        //assertThat(context).getBean(Connector.class).as("Tomcat Catalina connector").isNull();
    }
}
