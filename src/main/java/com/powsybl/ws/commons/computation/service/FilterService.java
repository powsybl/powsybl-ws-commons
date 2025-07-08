package com.powsybl.ws.commons.computation.service;

import com.powsybl.ws.commons.computation.dto.GlobalFilter;
import com.powsybl.ws.commons.computation.dto.ResourceFilterDTO;
import org.gridsuite.filter.FilterLoader;

import java.util.List;
import java.util.UUID;

public interface FilterService extends FilterLoader {
    List<ResourceFilterDTO> getResourceFilters(UUID networkUuid, String variantId, GlobalFilter globalFilter);
}
