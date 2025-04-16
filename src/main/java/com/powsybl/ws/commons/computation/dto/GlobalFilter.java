/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.dto;

import com.powsybl.iidm.network.Country;
import com.powsybl.security.LimitViolationType;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * @author maissa Souissi <maissa.souissi at rte-france.com>
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GlobalFilter {
    List<String> nominalV;

    List<Country> countryCode;

    List<UUID> genericFilter;

    List<LimitViolationType> limitViolationsTypes;
}
