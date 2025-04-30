/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation;

import java.util.Objects;

/**
 * @author Anis Touri <anis.touri at rte-france.com>
 */

public class ComputationException extends RuntimeException {
    public enum Type {
        RESULT_NOT_FOUND,
        INVALID_FILTER_FORMAT,
        INVALID_SORT_FORMAT,
        INVALID_FILTER,
        NETWORK_NOT_FOUND,
        PARAMETERS_NOT_FOUND,
        FILE_EXPORT_ERROR,
        EVALUATE_FILTER_FAILED,
        LIMIT_REDUCTION_CONFIG_ERROR,
        SPECIFIC,
    }

    private final Type type;

    public ComputationException(Type type) {
        super(Objects.requireNonNull(type.name()));
        this.type = type;
    }

    public ComputationException(String message) {
        super(message);
        this.type = Type.SPECIFIC;
    }

    public ComputationException(String message, Throwable cause) {
        super(message, cause);
        this.type = Type.SPECIFIC;
    }

    public ComputationException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
