package com.powsybl.ws.commons.computation.config;

import com.fasterxml.jackson.databind.InjectableValues;
import com.powsybl.commons.report.ReportNodeDeserializer;
import com.powsybl.commons.report.ReportNodeJsonModule;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties({ ComputationProperties.class })
public class ComputationConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.modulesToInstall(new ReportNodeJsonModule())
                .postConfigurer(objMapper -> objMapper.setInjectableValues(new InjectableValues.Std().addValue(ReportNodeDeserializer.DICTIONARY_VALUE_ID, null)));
    }

    public static final String COMPUTATION_TASK_EXECUTOR_BEAN = "computationTaskExecutor";

    @Bean
    public ComputationManager computationManager(@Qualifier(COMPUTATION_TASK_EXECUTOR_BEAN) ThreadPoolTaskExecutor taskExecutor) throws IOException {
        return new LocalComputationManager(taskExecutor);
    }

    // note: the shutdown is managed by spring
    @Bean(name = COMPUTATION_TASK_EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor computationTaskExecutor(final ComputationProperties props) {
        return new ThreadPoolTaskExecutorBuilder()
                //.additionalCustomizers()
                //.customizers()
                //.taskDecorator()
                .threadNamePrefix(props.getExecutor().getThreadNamePrefix())
                .awaitTermination(props.getExecutor().getShutdown().isAwaitTermination())
                .awaitTerminationPeriod(props.getExecutor().getShutdown().getAwaitTerminationPeriod())
                .acceptTasksAfterContextClose(props.getExecutor().getPool().getShutdown().isAcceptTasksAfterContextClose())
                .allowCoreThreadTimeOut(props.getExecutor().getPool().isAllowCoreThreadTimeout())
                .corePoolSize(props.getExecutor().getPool().getCoreSize())
                .maxPoolSize(props.getExecutor().getPool().getMaxSize())
                .queueCapacity(props.getExecutor().getPool().getQueueCapacity())
                .keepAlive(props.getExecutor().getPool().getKeepAlive())
                .build();
    }

    /* Spring auto-configuration use an @ConditionalOnMissingBean({Executor.class}) which detect our second executor
     * so we need to manually enable it to not have unwanted task in the second executor.
     *
     */
    // TODO: force org.springframework.boot.autoconfigure.task.TaskExecutorConfigurations.TaskExecutorConfiguration to exec
}
















