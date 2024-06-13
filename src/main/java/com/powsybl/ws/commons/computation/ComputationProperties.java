package com.powsybl.ws.commons.computation;

import lombok.Data;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for analysis servers
 */
@Data
@ConfigurationProperties(prefix = "powsybl-ws.computation")
public class ComputationProperties {
    /**
     * Configuration properties for computation-specific tasks (separated of standard one of Spring).
     */
    @NestedConfigurationProperty
    private TaskExecutionProperties taskExecution = new TaskExecutionProperties();
}
