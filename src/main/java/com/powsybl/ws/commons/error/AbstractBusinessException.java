/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import lombok.NonNull;

import java.util.Map;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 *
 * Base runtime exception for Powsybl-ws services enriched with a business error code and,
 * optionally, a remote {@link PowsyblWsProblemDetail}.
 */
public abstract class AbstractBusinessException extends RuntimeException {

    protected AbstractBusinessException(String message) {
        super(message);
    }

    protected AbstractBusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @return the business error code associated with the exception when available.
     */
    public abstract @NonNull BusinessErrorCode getBusinessErrorCode();

    /**
     * @return additional values that can be propagated with the problem detail.
     */
    public @NonNull Map<String, Object> getBusinessErrorValues() {
        return Map.of();
    }
}
