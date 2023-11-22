/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

import com.powsybl.ws.commons.springboot.PowsyblWsCommonAutoConfiguration;
import com.powsybl.ws_common_spring_test.SpringBootApplicationForTest;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * We test in this class the presence or absence of our module, not the content of the configuration.
 */
@DisplayNameGeneration(DisplayNameGenerator.Simple.class)
class SpringBootAutoConfigurationTest implements WithAssertions {
    /*
     * We can't test if org.springframework.boot.autoconfigure.AutoConfiguration.imports
     * is detected because we aren't in a JAR during our tests...
     * so we limit tests to the effects of configuration values.
     */
    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootApplicationForTest.class, PowsyblWsCommonAutoConfiguration.class));

    @Test
    void testNormally() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PowsyblWsCommonAutoConfiguration.class);
            //in non-web context, our configuration shouldn't trigger the creation of web beans
            assertThat(context).doesNotHaveBean(WebServerFactoryCustomizer.class);
            assertThat(context).doesNotHaveBean(ConfigurableTomcatWebServerFactory.class);
        });
    }
}
