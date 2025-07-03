/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.computation.utils;

import com.google.common.collect.Lists;
import com.powsybl.ws.commons.computation.dto.ResourceFilterDTO;
import jakarta.persistence.criteria.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.EscapeCharacter;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.springframework.data.jpa.domain.Specification.anyOf;
import static org.springframework.data.jpa.domain.Specification.not;

/**
 * Utility class to create Spring Data JPA Specification (Spring interface for JPA Criteria API).
 *
 * @author Kevin Le Saulnier <kevin.lesaulnier@rte-france.com>
 */
public final class SpecificationUtils {
    private static final int MAX_IN_CLAUSE_SIZE = 500;

    public static final String FIELD_SEPARATOR = ".";

    // Utility class, so no constructor
    private SpecificationUtils() { }

    // we use .as(String.class) to be able to works on enum fields
    public static <X> Specification<X> equals(String field, String value) {
        return (root, cq, cb) -> cb.equal(
                cb
                    .upper(getColumnPath(root, field).as(String.class))
                    .as(String.class),
                value.toUpperCase()
        );
    }

    public static <X> Specification<X> notEqual(String field, String value) {
        return (root, cq, cb) -> cb.notEqual(getColumnPath(root, field), value);
    }

    public static <X> Specification<X> contains(String field, String value) {
        return (root, cq, cb) -> cb.like(cb.upper(getColumnPath(root, field).as(String.class)), "%" + EscapeCharacter.DEFAULT.escape(value).toUpperCase() + "%", EscapeCharacter.DEFAULT.getEscapeCharacter());
    }

    public static <X> Specification<X> startsWith(String field, String value) {
        return (root, cq, cb) -> cb.like(cb.upper(getColumnPath(root, field).as(String.class)), EscapeCharacter.DEFAULT.escape(value).toUpperCase() + "%", EscapeCharacter.DEFAULT.getEscapeCharacter());
    }

    /**
     * Returns a specification where the field value is not equal within the given tolerance.
     */
    public static <X> Specification<X> notEqual(String field, Double value, Double tolerance) {
        return (root, cq, cb) -> {
            Expression<Double> doubleExpression = getColumnPath(root, field).as(Double.class);
            /*
             * in order to be equal to doubleExpression, value has to fit :
             * value - tolerance <= doubleExpression <= value + tolerance
             * therefore in order to be different at least one of the opposite comparison needs to be true :
             */
            return cb.or(
                    cb.greaterThan(doubleExpression, value + tolerance),
                    cb.lessThan(doubleExpression, value - tolerance)
            );
        };
    }

    public static <X> Specification<X> lessThanOrEqual(String field, Double value, Double tolerance) {
        return (root, cq, cb) -> {
            Expression<Double> doubleExpression = getColumnPath(root, field).as(Double.class);
            return cb.lessThanOrEqualTo(doubleExpression, value + tolerance);
        };
    }

    public static <X> Specification<X> greaterThanOrEqual(String field, Double value, Double tolerance) {
        return (root, cq, cb) -> {
            Expression<Double> doubleExpression = getColumnPath(root, field).as(Double.class);
            return cb.greaterThanOrEqualTo(doubleExpression, value - tolerance);
        };
    }

    public static <X> Specification<X> isNotEmpty(String field) {
        return (root, cq, cb) -> cb.isNotEmpty(getColumnPath(root, field));
    }

    public static <X> Specification<X> distinct() {
        return (root, cq, cb) -> {
            // to select distinct result, we need to set a "criteria query" param
            // we don't need to return any predicate here
            cq.distinct(true);
            return null;
        };
    }

    public static <X> Specification<X> appendFiltersToSpecification(Specification<X> specification, List<ResourceFilterDTO> resourceFilters) {
        Objects.requireNonNull(specification);

        if (resourceFilters == null || resourceFilters.isEmpty()) {
            return specification;
        }

        Specification<X> completedSpecification = specification;

        for (ResourceFilterDTO resourceFilter : resourceFilters) {
            if (resourceFilter.dataType() == ResourceFilterDTO.DataType.TEXT) {
                completedSpecification = appendTextFilterToSpecification(completedSpecification, resourceFilter);
            } else if (resourceFilter.dataType() == ResourceFilterDTO.DataType.NUMBER) {
                completedSpecification = appendNumberFilterToSpecification(completedSpecification, resourceFilter);
            }
        }

        return completedSpecification;
    }

    @NotNull
    private static <X> Specification<X> appendTextFilterToSpecification(Specification<X> specification, ResourceFilterDTO resourceFilter) {
        Specification<X> completedSpecification = specification;

        switch (resourceFilter.type()) {
            case NOT_EQUAL, EQUALS, IN -> {
                // this type can manage one value or a list of values (with OR)
                if (resourceFilter.value() instanceof Collection<?> valueList) {
                    // implicitely an IN resourceFilter type because only IN may have value lists as filter value
                    completedSpecification = completedSpecification.and(generateInSpecification(resourceFilter.column(), (List<String>) valueList));
                } else if (resourceFilter.value() == null) {
                    // if the value is null, we build an impossible specification (trick to remove later on ?)
                    completedSpecification = completedSpecification.and(not(completedSpecification));
                } else {
                    completedSpecification = completedSpecification.and(equals(resourceFilter.column(), resourceFilter.value().toString()));
                }
            }
            case CONTAINS -> {
                if (resourceFilter.value() instanceof Collection<?> valueList) {
                    completedSpecification = completedSpecification.and(
                            anyOf(
                                    valueList
                                            .stream()
                                            .map(value -> SpecificationUtils.<X>contains(resourceFilter.column(), value.toString()))
                                            .toList()
                            ));
                } else {
                    completedSpecification = completedSpecification.and(contains(resourceFilter.column(), resourceFilter.value().toString()));
                }
            }
            case STARTS_WITH ->
                    completedSpecification = completedSpecification.and(startsWith(resourceFilter.column(), resourceFilter.value().toString()));
            default -> throw new IllegalArgumentException("The filter type " + resourceFilter.type() + " is not supported with the data type " + resourceFilter.dataType());
        }

        return completedSpecification;
    }

    private static <X> Specification<X> generateInSpecification(String column, List<String> inPossibleValues) {
        if (inPossibleValues.size() > MAX_IN_CLAUSE_SIZE) {
            // there are too many values for only one call to anyOf() : it might cause a StackOverflow
            // => the specification is divided into several specifications which have an OR between them :
            List<List<String>> chunksOfInValues = Lists.partition(inPossibleValues, MAX_IN_CLAUSE_SIZE);
            Specification<X> containerSpec = null;
            for (List<String> chunk : chunksOfInValues) {
                Specification<X> multiOrEqualSpec = anyOf(
                        chunk
                                .stream()
                                .map(value -> SpecificationUtils.<X>equals(column, value))
                                .toList()
                );
                if (containerSpec == null) {
                    containerSpec = multiOrEqualSpec;
                } else {
                    containerSpec = containerSpec.or(multiOrEqualSpec);
                }
            }
            return containerSpec;
        }
        return anyOf(inPossibleValues
                        .stream()
                        .map(value -> SpecificationUtils.<X>equals(column, value))
                        .toList()
        );
    }

    @NotNull
    private static <X> Specification<X> appendNumberFilterToSpecification(Specification<X> specification, ResourceFilterDTO resourceFilter) {
        String filterValue = resourceFilter.value().toString();
        double tolerance;
        if (resourceFilter.tolerance() != null) {
            tolerance = resourceFilter.tolerance();
        } else {
            // the reference for the comparison is the number of digits after the decimal point in filterValue
            // extra digits are ignored, but the user may add '0's after the decimal point in order to get a better precision
            String[] splitValue = filterValue.split("\\.");
            int numberOfDecimalAfterDot = 0;
            if (splitValue.length > 1) {
                numberOfDecimalAfterDot = splitValue[1].length();
            }
            // tolerance is multiplied by 0.5 to simulate the fact that the database value is rounded (in the front, from the user viewpoint)
            // more than 13 decimal after dot will likely cause rounding errors due to double precision
            tolerance = Math.pow(10, -numberOfDecimalAfterDot) * 0.5;
        }
        Double valueDouble = Double.valueOf(filterValue);
        return switch (resourceFilter.type()) {
            case NOT_EQUAL -> specification.and(notEqual(resourceFilter.column(), valueDouble, tolerance));
            case LESS_THAN_OR_EQUAL ->
                    specification.and(lessThanOrEqual(resourceFilter.column(), valueDouble, tolerance));
            case GREATER_THAN_OR_EQUAL ->
                    specification.and(greaterThanOrEqual(resourceFilter.column(), valueDouble, tolerance));
            default ->
                    throw new IllegalArgumentException("The filter type " + resourceFilter.type() + " is not supported with the data type " + resourceFilter.dataType());
        };
    }

    /**
     * This method allow to query eventually dot separated fields with the Criteria API
     * Ex : from 'fortescueCurrent.positiveMagnitude' we create the query path
     * root.get("fortescueCurrent").get("positiveMagnitude") to access to the correct nested field
     *
     * @param root               the root entity
     * @param dotSeparatedFields dot separated fields (can be only one field without any dot)
     * @param <X>                the entity type referenced by the root
     * @param <Y>                the type referenced by the path
     * @return path for the query
     */
    private static <X, Y> Path<Y> getColumnPath(Root<X> root, String dotSeparatedFields) {
        if (dotSeparatedFields.contains(SpecificationUtils.FIELD_SEPARATOR)) {
            String[] fields = dotSeparatedFields.split("\\.");
            Path<Y> path = root.get(fields[0]);
            for (int i = 1; i < fields.length; i++) {
                path = path.get(fields[i]);
            }
            return path;
        } else {
            return root.get(dotSeparatedFields);
        }
    }
}
