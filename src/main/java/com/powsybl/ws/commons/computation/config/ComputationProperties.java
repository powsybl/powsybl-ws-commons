package com.powsybl.ws.commons.computation.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("powsybl.computation")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ComputationProperties {
    /**
     * Like {@code "spring.task.execution"} properties.
     * @see TaskExecutionProperties
     */
    private ComputationExecutorProperties executor = new ComputationExecutorProperties(); // Inherits core pool size, max pool size, etc.

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ComputationExecutorProperties {
        private TaskExecutionProperties.Pool pool = new TaskExecutionProperties.Pool();
        private TaskExecutionProperties.Shutdown shutdown = new TaskExecutionProperties.Shutdown();
        /**
         * Prefix to use for the names of newly created threads.
         */
        private String threadNamePrefix = "powsybl-computation-";
    }
}
