package com.powsybl.ws.commons.computation.specification;

import com.powsybl.ws.commons.computation.dto.ResourceFilterDTO;
import com.powsybl.ws.commons.computation.utils.SpecificationUtils;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static com.powsybl.ws.commons.computation.dto.ResourceFilterDTO.DataType.NUMBER;
import static com.powsybl.ws.commons.computation.dto.ResourceFilterDTO.DataType.TEXT;
import static com.powsybl.ws.commons.computation.dto.ResourceFilterDTO.Type.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class CommonSpecificationBuilderTest {

    UUID resUuid = UUID.randomUUID();
    CommonSpecificationBuilderTestImpl builder;
    CriteriaBuilder cb;
    Root<Object> root;
    Path<Object> path;
    Expression<String> exprString;
    Expression<Double> exprDouble;

    @BeforeEach
    void init() {
        builder = new CommonSpecificationBuilderTestImpl();
        cb = Mockito.mock(CriteriaBuilder.class);
        root = Mockito.mock(Root.class);
        path = Mockito.mock(Path.class);
        exprString = Mockito.mock(Expression.class);

        // Configure mocks' behavior
        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(any(Path.class), any(UUID.class))).thenReturn(Mockito.mock(Predicate.class));
        when(path.get(anyString())).thenReturn(path);
        when(path.as(String.class)).thenReturn(exprString);
        when(path.as(Double.class)).thenReturn(exprDouble);
        when(cb.upper(exprString)).thenReturn(exprString);
        when(exprString.as(String.class)).thenReturn(exprString);
    }

    @Test
    public void testResultUuidEquals() {
        CriteriaBuilder cb = Mockito.mock(CriteriaBuilder.class);

        when(root.get("resultId")).thenReturn(path);
        when(cb.equal(path, resUuid)).thenReturn(Mockito.mock(Predicate.class));

        var specification = builder.resultUuidEquals(resUuid);
        assertNotNull(specification);
    }

    @Test
    public void testBuildSpecification() {
        CriteriaQuery<?> cq = Mockito.mock(CriteriaQuery.class);

        // test data
        List<ResourceFilterDTO> resourceFilters = List.of(
                new ResourceFilterDTO(TEXT, EQUALS, "dummyColumnValue", "dummyColumn"),
                new ResourceFilterDTO(TEXT, STARTS_WITH, "dum", "dummyColumn"),
                new ResourceFilterDTO(TEXT, IN, List.of("dummyColumnValue", "otherDummyColumnValue"), "dummyColumn"),
                new ResourceFilterDTO(TEXT, CONTAINS, "partialValue", "dummyColumn"),
                new ResourceFilterDTO(TEXT, CONTAINS, List.of("partialValue1", "partialValue2"), "dummyColumn"),
                new ResourceFilterDTO(NUMBER, LESS_THAN_OR_EQUAL, 100.0157, "dummyNumberColumn"),
                new ResourceFilterDTO(NUMBER, GREATER_THAN_OR_EQUAL, 10, "dummyNumberColumn", 0.1),
                new ResourceFilterDTO(NUMBER, NOT_EQUAL, 10, "parent.dummyNumberColumn")
        );
        List<ResourceFilterDTO> resourceFiltersWithChildren = List.of(
                new ResourceFilterDTO(NUMBER, NOT_EQUAL, 10, "parent.dummyNumberColumn")
        );
        List<ResourceFilterDTO> emptyResourceFilters = List.of();

        var specification = builder.buildSpecification(resUuid, resourceFilters, true);
        assertNotNull(specification);
        Predicate predicate = specification.toPredicate(root, cq, cb);
        assertNotNull(predicate);

        var specificationWithChildren = builder.buildSpecification(resUuid, resourceFiltersWithChildren, false);
        assertNotNull(specificationWithChildren);
        Predicate predicateWithChildren = specificationWithChildren.toPredicate(root, cq, cb);
        assertNotNull(predicateWithChildren);

        var emptySpec = builder.buildSpecification(resUuid, emptyResourceFilters, false);
        assertNotNull(emptySpec);
        Predicate emptyPred = emptySpec.toPredicate(root, cq, cb);
        assertNotNull(emptyPred);
    }

    @Test
    public void testBuildLimitViolationsSpecification() {
        List<ResourceFilterDTO> resourceFilters = List.of(
                new ResourceFilterDTO(NUMBER, NOT_EQUAL, 10, "dummyNumberColumn")
        );

        var specification = builder.buildLimitViolationsSpecification(List.of(resUuid), resourceFilters);
        assertNotNull(specification);
    }

    @Test
    public void testInvalidResourceFilters() {
        UUID resultUuid = UUID.randomUUID();
        List<ResourceFilterDTO> textResourceFilters = List.of(
                new ResourceFilterDTO(TEXT, GREATER_THAN_OR_EQUAL, "dummyValue", "dummyColumn")
        );
        assertThrows(IllegalArgumentException.class, () -> builder.buildSpecification(resultUuid, textResourceFilters, true));

        List<ResourceFilterDTO> numResourceFilters = List.of(
                new ResourceFilterDTO(NUMBER, IN, 1, "dummyColumn")
        );
        assertThrows(IllegalArgumentException.class, () -> builder.buildSpecification(resultUuid, numResourceFilters, true));
    }

    // test specific dummy implementation
    private static class CommonSpecificationBuilderTestImpl extends AbstractCommonSpecificationBuilder<Object> {

        @Override
        public boolean isNotParentFilter(ResourceFilterDTO filter) {
            return !filter.column().equals("parent.dummyNumberColumn");
        }

        @Override
        public String getIdFieldName() {
            return "id";
        }

        @Override
        public Path<UUID> getResultIdPath(Root<Object> root) {
            return root.get("resultId");
        }

        @Override
        public Specification<Object> addSpecificFilterWhenNoChildrenFilter() {
            return SpecificationUtils.isNotEmpty("dummyColumn");
        }

        @Override
        public Specification<Object> addSpecificFilterWhenChildrenFilters() {
            return addSpecificFilterWhenNoChildrenFilter();
        }
    }
}
