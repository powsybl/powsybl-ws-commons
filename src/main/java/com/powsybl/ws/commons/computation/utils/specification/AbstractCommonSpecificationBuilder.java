/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.utils.specification;

import com.powsybl.ws.commons.computation.dto.ResourceFilterDTO;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

/**
 * @author Kevin LE SAULNIER <kevin.lesaulnier@rte-france.com>
 */
@NoArgsConstructor
public abstract class AbstractCommonSpecificationBuilder<T> {

    public Specification<T> resultUuidEquals(UUID value) {
        return (root, cq, cb) -> cb.equal(getResultIdPath(root), value);
    }

    public Specification<T> uuidIn(List<UUID> uuids) {
        return (root, cq, cb) -> root.get(getIdFieldName()).in(uuids);
    }

    public Specification<T> buildSpecification(UUID resultUuid, List<ResourceFilterDTO> resourceFilters, boolean withJoin) {
        List<ResourceFilterDTO> childrenResourceFilters = resourceFilters.stream().filter(this::isNotParentFilter).toList();
        // since sql joins generates duplicate results, we need to use distinct here
        Specification<T> specification = SpecificationUtils.distinct();
        // filter by resultUuid
        specification = specification.and(Specification.where(resultUuidEquals(resultUuid)));
        if (childrenResourceFilters.isEmpty()) {
            specification = specification.and(addSpecificFilterWhenNoChildrenFilter());
        } else {
            if (withJoin) { // TODO : n'était pas présent pour les analyse de sécu
                SpecificationUtils.appendFiltersToSpecification(specification, childrenResourceFilters);
                /*childrenFilters
                            .forEach(filter -> SpecificationUtils.addPredicate(criteriaBuilder, root.get(SLACK_BUS_RESULTS), predicates, filter));*/ // TODO : SLACK_BUS_RESULTS ??
            } else {
                // needed here to filter main entities that would have empty collection when filters are applied
                specification = specification.and(addSpecificFilterWhenChildrenFilters()); // TODO => super bizarre que rien n'est fait avec les children, cf dans la version loadflow
            }
        }

        return SpecificationUtils.appendFiltersToSpecification(specification, resourceFilters);
    }

    public Specification<T> buildLimitViolationsSpecification(List<UUID> uuids, List<ResourceFilterDTO> resourceFilters) {
        List<ResourceFilterDTO> childrenResourceFilter = resourceFilters.stream().filter(this::isNotParentFilter).toList();
        Specification<T> specification = Specification.where(uuidIn(uuids));

        return SpecificationUtils.appendFiltersToSpecification(specification, childrenResourceFilter);
    }

    public abstract Specification<T> addSpecificFilterWhenChildrenFilters();

    public abstract boolean isNotParentFilter(ResourceFilterDTO filter);

    public abstract String getIdFieldName();

    public abstract Path<UUID> getResultIdPath(Root<T> root);

    public abstract Specification<T> addSpecificFilterWhenNoChildrenFilter();
}

