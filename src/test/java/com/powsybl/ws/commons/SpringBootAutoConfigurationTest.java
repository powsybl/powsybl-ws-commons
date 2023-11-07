package com.powsybl.ws.commons;

import com.powsybl.ws.commons.springboot.PowsyblWsCommonAutoConfiguration;
import com.powsybl.ws_common_spring_test.SpringBootApplicationForTest;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

@DisplayNameGeneration(DisplayNameGenerator.Simple.class)
class SpringBootAutoConfigurationTest implements WithAssertions {
    /*
     * We can't test if spring.factories or org.springframework.boot.autoconfigure.AutoConfiguration.imports
     * is detected because we aren't in a JAR during our tests...
     * so we limit tests to the configuration of the contexts.
     */
    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootApplicationForTest.class, PowsyblWsCommonAutoConfiguration.class));

    @Test
    void testNormally() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
            assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class);
            assertThat(context).doesNotHaveBean(ConfigurableTomcatWebServerFactory.class);
        });
    }

    @Disabled("Can't test that currently because not from JAR")
    @Test
    void autoConfigurationIsIgnoredIfClassNotPresent() {
        this.contextRunner
            .withClassLoader(new FilteredClassLoader(PowsyblWsCommonAutoConfiguration.class))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(PowsyblWsCommonAutoConfiguration.class);
            });
    }

    @Test
    void testWithInvalidSkipProperty() {
        this.contextRunner.withPropertyValues("powsybl-ws.common.skip-init=foo")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(PowsyblWsCommonAutoConfiguration.class);
            });
    }

    @Test
    void testWhenDisabledByProperty() {
        this.contextRunner
            .withPropertyValues("powsybl-ws.common.skip-init=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(PowsyblWsCommonAutoConfiguration.class);
            });
    }
}
