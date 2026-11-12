package com.powsybl.ws.commons;

import com.powsybl.ws.commons.springboot.PowsyblWsCommonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.client.RestClientCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringBootAutoConfigurationRestClientTest {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PowsyblWsCommonAutoConfiguration.class));

    @Test
    void shouldConfigureServletSecurityWhenEnabled() {
        webContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
            assertThat(context).hasSingleBean(RestClientCustomizer.class);
        });
    }
}
