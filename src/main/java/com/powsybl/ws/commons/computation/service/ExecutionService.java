/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.computation.service;

import com.powsybl.computation.ComputationManager;
import com.powsybl.ws.commons.computation.config.ComputationConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;


/**
 * @deprecated Use directly Spring beans {@code "computationTaskExecutor"} and {@link ComputationManager}.
 */
@Deprecated(since = "1.24.0", forRemoval = true)
@Service
@AllArgsConstructor
@Getter
public class ExecutionService {
    private final ExecutorService executorService;
    private final ComputationManager computationManager;

    public ExecutionService(@Qualifier(ComputationConfig.COMPUTATION_TASK_EXECUTOR_BEAN) final TaskExecutor executorService, final ComputationManager computationManager) {
        this(new ExecutorServiceAdapter(executorService), computationManager);
    }
}
