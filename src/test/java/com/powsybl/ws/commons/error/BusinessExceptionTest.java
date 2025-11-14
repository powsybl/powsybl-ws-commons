/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.error;

import lombok.NonNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class BusinessExceptionTest {

    @Test
    void businessErrorValuesAreEmpty() {
        AbstractBusinessException exception = new AbstractBusinessException("no business error values") {
            @Override
            public @NonNull BusinessErrorCode getBusinessErrorCode() {
                return () -> "test.default";
            }
        };

        assertThat(exception.getBusinessErrorValues()).isEmpty();
    }
}
