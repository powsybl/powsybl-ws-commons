package com.powsybl.ws.commons.computation;

import com.powsybl.commons.report.ReportNodeJsonModule;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableConfigurationProperties({ ComputationProperties.class })
@EnableAsync
public class ComputationConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.build().registerModule(new ReportNodeJsonModule());
    }

    /**
     * @see org.springframework.boot.autoconfigure.task.TaskExecutionProperties <code>spring.task.execution.*</code>
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Lazy //some analysis servers don't use it
    public ComputationManager computationManager(@Autowired TaskExecutor taskExecutor) {
        try {
            return new LocalComputationManager(taskExecutor);
        } catch (final Exception ex) {
            throw new BeanCreationException("computationManager", "Error while creating computation manager", ex);
        }
    }
}
