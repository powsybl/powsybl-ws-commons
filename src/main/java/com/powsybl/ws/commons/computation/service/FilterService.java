/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.service;

import com.powsybl.ws.commons.computation.dto.GlobalFilter;
import com.powsybl.ws.commons.computation.dto.ResourceFilterDTO;
import org.gridsuite.filter.FilterLoader;
import org.gridsuite.filter.utils.EquipmentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Rehili Ghazwa <ghazwa.rehili at rte-france.com>
 */
public interface FilterService extends FilterLoader {
    Optional<ResourceFilterDTO> getResourceFilter(UUID networkUuid, String variantId,
                                                  GlobalFilter globalFilter, List<EquipmentType> equipmentTypes,
                                                  String columnName);
}
