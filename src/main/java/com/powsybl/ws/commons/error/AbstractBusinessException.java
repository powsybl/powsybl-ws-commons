/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import java.util.Optional;

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
    public abstract BusinessErrorCode getBusinessErrorCode();

    /**
     * @return an optional remote {@link PowsyblWsProblemDetail} when the exception wraps a downstream error.
     */
    public Optional<PowsyblWsProblemDetail> getRemoteError() {
        return Optional.empty();
    }
}
