/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.computation.service;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.ws.commons.computation.dto.GlobalFilter;
import org.gridsuite.filter.AbstractFilter;
import org.gridsuite.filter.expertfilter.ExpertFilter;
import org.gridsuite.filter.expertfilter.expertrule.*;
import org.gridsuite.filter.identifierlistfilter.IdentifiableAttributes;
import org.gridsuite.filter.utils.EquipmentType;
import org.gridsuite.filter.utils.FilterServiceUtils;
import org.gridsuite.filter.utils.expertfilter.CombinatorType;
import org.gridsuite.filter.utils.expertfilter.FieldType;
import org.gridsuite.filter.utils.expertfilter.OperatorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * @author Rehili Ghazwa <ghazwa.rehili at rte-france.com>
 */
@ExtendWith(MockitoExtension.class)
class FilterServiceTest {

    @Mock
    private NetworkStoreService networkStoreService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Network network;

    @Mock
    private VariantManager variantManager;

    @Mock
    private AbstractFilterService filterService;

    private static final String FILTER_SERVER_BASE_URI = "http://localhost:8080";
    private static final String VARIANT_ID = "testVariant";
    private static final UUID NETWORK_UUID = UUID.randomUUID();
    private static final UUID FILTER_UUID = UUID.randomUUID();

    @Test
    void shouldReturnEmptyListWhenFiltersUuidsIsEmpty() {
        // Given
        when(filterService.getFilters(anyList())).thenCallRealMethod();
        List<UUID> emptyList = Collections.emptyList();

        // When
        List<AbstractFilter> result = filterService.getFilters(emptyList);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenFiltersUuidsIsNull() {
        // Given
        when(filterService.getFilters(any())).thenCallRealMethod();

        // When
        List<AbstractFilter> result = filterService.getFilters(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void houldCallRestTemplateAndReturnFilters() {
        // Given
        when(filterService.getFilters(anyList())).thenCallRealMethod();
        ReflectionTestUtils.setField(filterService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(filterService, "filterServerBaseUri", FILTER_SERVER_BASE_URI);

        List<UUID> filterUuids = List.of(FILTER_UUID);
        List<AbstractFilter> expectedFilters = Collections.singletonList(mock(AbstractFilter.class));

        ResponseEntity<List<AbstractFilter>> responseEntity = new ResponseEntity<>(expectedFilters, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // When
        List<AbstractFilter> result = filterService.getFilters(filterUuids);

        // Then
        assertEquals(expectedFilters, result);
        verify(restTemplate).exchange(
                contains("v1/filters/metadata"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void shouldThrowPowsyblExceptionWhenHttpError() {
        // Given
        when(filterService.getFilters(anyList())).thenCallRealMethod();
        ReflectionTestUtils.setField(filterService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(filterService, "filterServerBaseUri", FILTER_SERVER_BASE_URI);

        List<UUID> filterUuids = List.of(FILTER_UUID);

        HttpStatusCodeException httpException = mock(HttpStatusCodeException.class);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(httpException);

        // When & Then
        PowsyblException exception = assertThrows(PowsyblException.class,
                () -> filterService.getFilters(filterUuids));

        assertTrue(exception.getMessage().contains("Filters not found"));
        assertTrue(exception.getMessage().contains(FILTER_UUID.toString()));
    }

    @Test
    void shouldReturnNetworkWhenSuccessful() {
        // Given
        when(filterService.getNetwork(any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(filterService, "networkStoreService", networkStoreService);

        when(networkStoreService.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION))
                .thenReturn(network);
        when(network.getVariantManager()).thenReturn(variantManager);

        // When
        Network result = filterService.getNetwork(NETWORK_UUID, VARIANT_ID);

        // Then
        assertEquals(network, result);
        verify(variantManager).setWorkingVariant(VARIANT_ID);
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenPowsyblException() {
        // Given
        when(filterService.getNetwork(any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(filterService, "networkStoreService", networkStoreService);

        PowsyblException powsyblException = new PowsyblException("Network not found");
        when(networkStoreService.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION))
                .thenThrow(powsyblException);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> filterService.getNetwork(NETWORK_UUID, VARIANT_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Network not found", exception.getReason());
    }

    @Test
    void shouldReturnEmptyListWhenNumberExpertRulesValuesIsNull() {
        // Given
        when(filterService.createNumberExpertRules(any(), any())).thenCallRealMethod();

        // When
        List<AbstractExpertRule> result = filterService.createNumberExpertRules(null, FieldType.NOMINAL_VOLTAGE);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCreateNumberExpertRulesWhenValuesProvided() {
        // Given
        when(filterService.createNumberExpertRules(any(), any())).thenCallRealMethod();
        List<String> values = Arrays.asList("400.0", "225.0");

        // When
        List<AbstractExpertRule> result = filterService.createNumberExpertRules(values, FieldType.NOMINAL_VOLTAGE);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        for (AbstractExpertRule rule : result) {
            assertInstanceOf(NumberExpertRule.class, rule);
            NumberExpertRule numberRule = (NumberExpertRule) rule;
            assertEquals(FieldType.NOMINAL_VOLTAGE, numberRule.getField());
            assertEquals(OperatorType.EQUALS, numberRule.getOperator());
        }
    }

    @Test
    void shouldCreateCorrectPropertiesRule() {
        // Given
        when(filterService.createPropertiesRule(any(), any(), any())).thenCallRealMethod();
        String property = "testProperty";
        List<String> values = Arrays.asList("value1", "value2");

        // When
        AbstractExpertRule result = filterService.createPropertiesRule(property, values, FieldType.SUBSTATION_PROPERTIES);

        // Then
        assertNotNull(result);
        assertInstanceOf(PropertiesExpertRule.class, result);

        PropertiesExpertRule propertiesRule = (PropertiesExpertRule) result;
        assertEquals(CombinatorType.OR, propertiesRule.getCombinator());
        assertEquals(OperatorType.IN, propertiesRule.getOperator());
        assertEquals(FieldType.SUBSTATION_PROPERTIES, propertiesRule.getField());
        assertEquals(property, propertiesRule.getPropertyName());
        assertEquals(values, propertiesRule.getPropertyValues());
    }

    @Test
    void shouldReturnEmptyListWhenEnumExpertRulesValuesIsNull() {
        // Given
        when(filterService.createEnumExpertRules(any(), any())).thenCallRealMethod();

        // When
        List<AbstractExpertRule> result = filterService.createEnumExpertRules(null, FieldType.COUNTRY);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCreateRulesWhenValuesProvided() {
        // Given
        when(filterService.createEnumExpertRules(any(), any())).thenCallRealMethod();
        List<Country> values = Arrays.asList(Country.FR, Country.DE);

        // When
        List<AbstractExpertRule> result = filterService.createEnumExpertRules(values, FieldType.COUNTRY);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        for (AbstractExpertRule rule : result) {
            assertInstanceOf(EnumExpertRule.class, rule);
            EnumExpertRule enumRule = (EnumExpertRule) rule;
            assertEquals(FieldType.COUNTRY, enumRule.getField());
            assertEquals(OperatorType.EQUALS, enumRule.getOperator());
        }
    }

    @Test
    void shouldCreateCombinatorRule() {
        // Given
        when(filterService.createCombination(any(), any())).thenCallRealMethod();
        List<AbstractExpertRule> rules = Collections.singletonList(mock(AbstractExpertRule.class));

        // When
        AbstractExpertRule result = filterService.createCombination(CombinatorType.AND, rules);

        // Then
        assertNotNull(result);
        assertInstanceOf(CombinatorExpertRule.class, result);

        CombinatorExpertRule combinatorRule = (CombinatorExpertRule) result;
        assertEquals(CombinatorType.AND, combinatorRule.getCombinator());
        assertEquals(rules, combinatorRule.getRules());
    }

    @Test
    void shouldReturnEmptyWhenRulesIsEmpty() {
        // Given
        when(filterService.createOrCombination(any())).thenCallRealMethod();
        List<AbstractExpertRule> rules = Collections.emptyList();

        // When
        Optional<AbstractExpertRule> result = filterService.createOrCombination(rules);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnSingleRuleWhenOnlyOneRule() {
        // Given
        when(filterService.createOrCombination(any())).thenCallRealMethod();

        AbstractExpertRule rule = mock(AbstractExpertRule.class);
        List<AbstractExpertRule> rules = Collections.singletonList(rule);

        // When
        Optional<AbstractExpertRule> result = filterService.createOrCombination(rules);

        // Then
        assertTrue(result.isPresent());
        assertEquals(rule, result.get());
    }

    @Test
    void shouldReturnCombinatorWhenMultipleRules() {
        // Given
        when(filterService.createOrCombination(any())).thenCallRealMethod();
        when(filterService.createCombination(any(), any())).thenCallRealMethod();

        List<AbstractExpertRule> rules = Arrays.asList(
                mock(AbstractExpertRule.class),
                mock(AbstractExpertRule.class)
        );

        // When
        Optional<AbstractExpertRule> result = filterService.createOrCombination(rules);

        // Then
        assertTrue(result.isPresent());
        assertInstanceOf(CombinatorExpertRule.class, result.get());

        CombinatorExpertRule combinatorRule = (CombinatorExpertRule) result.get();
        assertEquals(CombinatorType.OR, combinatorRule.getCombinator());
    }

    @Test
    void shouldReturnEmptyWhenCombineFilterResultsInputIsEmpty() {
        // Given
        when(filterService.combineFilterResults(any(), anyBoolean())).thenCallRealMethod();
        List<List<String>> filterResults = Collections.emptyList();

        // When
        List<String> result = filterService.combineFilterResults(filterResults, true);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnIntersectionWhenUsingAndLogic() {
        // Given
        when(filterService.combineFilterResults(any(), anyBoolean())).thenCallRealMethod();
        List<List<String>> filterResults = Arrays.asList(
                Arrays.asList("item1", "item2", "item3"),
                Arrays.asList("item2", "item3", "item4"),
                Arrays.asList("item2", "item5")
        );

        // When
        List<String> result = filterService.combineFilterResults(filterResults, true);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains("item2"));
    }

    @Test
    void shouldReturnUnionWhenUsingOrLogic() {
        // Given
        when(filterService.combineFilterResults(any(), anyBoolean())).thenCallRealMethod();
        List<List<String>> filterResults = Arrays.asList(
                Arrays.asList("item1", "item2"),
                Arrays.asList("item3", "item4"),
                List.of("item5")
        );

        // When
        List<String> result = filterService.combineFilterResults(filterResults, false);

        // Then
        assertEquals(5, result.size());
        assertTrue(result.containsAll(Arrays.asList("item1", "item2", "item3", "item4", "item5")));
    }

    @Test
    void shouldReturnIdsFromFilteredNetwork() {
        // Given
        when(filterService.filterNetwork(any(), any())).thenCallRealMethod();
        AbstractFilter filter = mock(AbstractFilter.class);

        try (MockedStatic<FilterServiceUtils> mockStatic = mockStatic(FilterServiceUtils.class)) {
            List<IdentifiableAttributes> attributes = Arrays.asList(
                    createIdentifiableAttributes("id1"),
                    createIdentifiableAttributes("id2")
            );

            mockStatic.when(() -> FilterServiceUtils.getIdentifiableAttributes(filter, network, filterService))
                    .thenReturn(attributes);

            // When
            List<String> result = filterService.filterNetwork(filter, network);

            // Then
            assertEquals(Arrays.asList("id1", "id2"), result);
        }
    }

    @Test
    void shouldCreateCorrectRuleForSideOne() {
        // Given
        when(filterService.createVoltageLevelIdRule(any(), any())).thenCallRealMethod();
        UUID filterUuid = UUID.randomUUID();

        // When
        AbstractExpertRule result = filterService.createVoltageLevelIdRule(filterUuid, TwoSides.ONE);

        // Then
        assertNotNull(result);
        assertInstanceOf(FilterUuidExpertRule.class, result);

        FilterUuidExpertRule rule = (FilterUuidExpertRule) result;
        assertEquals(OperatorType.IS_PART_OF, rule.getOperator());
        assertEquals(FieldType.VOLTAGE_LEVEL_ID_1, rule.getField());
        assertTrue(rule.getValues().contains(filterUuid.toString()));
    }

    @Test
    void shouldCreateCorrectRuleForSideTwo() {
        // Given
        when(filterService.createVoltageLevelIdRule(any(), any())).thenCallRealMethod();
        UUID filterUuid = UUID.randomUUID();

        // When
        AbstractExpertRule result = filterService.createVoltageLevelIdRule(filterUuid, TwoSides.TWO);

        // Then
        assertNotNull(result);
        assertInstanceOf(FilterUuidExpertRule.class, result);

        FilterUuidExpertRule rule = (FilterUuidExpertRule) result;
        assertEquals(OperatorType.IS_PART_OF, rule.getOperator());
        assertEquals(FieldType.VOLTAGE_LEVEL_ID_2, rule.getField());
        assertTrue(rule.getValues().contains(filterUuid.toString()));
    }

    @Test
    void shouldReturnEmptyWhenNoExpertFiltersProvided() {
        // Given
        when(filterService.buildAllExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.buildNominalVoltageRules(any(), any())).thenCallRealMethod();

        GlobalFilter globalFilter = mock(GlobalFilter.class);
        when(globalFilter.getNominalV()).thenReturn(null);
        when(globalFilter.getCountryCode()).thenReturn(null);
        when(globalFilter.getSubstationProperty()).thenReturn(null);

        // When
        List<AbstractExpertRule> result = filterService.buildAllExpertRules(globalFilter, EquipmentType.GENERATOR);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnRulesWhenFilterExpertRulesProvided() {
        // Given
        when(filterService.buildAllExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.buildNominalVoltageRules(any(), any())).thenCallRealMethod();
        when(filterService.buildCountryCodeRules(any(), any())).thenCallRealMethod();
        when(filterService.buildSubstationPropertyRules(any(), any())).thenCallRealMethod();
        when(filterService.getNominalVoltageFieldType(any())).thenReturn(List.of(FieldType.NOMINAL_VOLTAGE));
        when(filterService.getCountryCodeFieldType(any())).thenReturn(List.of(FieldType.COUNTRY));
        when(filterService.getSubstationPropertiesFieldTypes(any())).thenReturn(List.of(FieldType.SUBSTATION_PROPERTIES));
        when(filterService.createNumberExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.createEnumExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.createPropertiesRule(any(), any(), any())).thenCallRealMethod();
        when(filterService.createOrCombination(any())).thenCallRealMethod();

        GlobalFilter globalFilter = mock(GlobalFilter.class);
        when(globalFilter.getNominalV()).thenReturn(List.of("400.0"));
        when(globalFilter.getCountryCode()).thenReturn(List.of(Country.FR));
        when(globalFilter.getSubstationProperty()).thenReturn(Map.of("prop1", List.of("value1")));

        // When
        List<AbstractExpertRule> result = filterService.buildAllExpertRules(globalFilter, EquipmentType.GENERATOR);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void shouldReturnEmptyWhenNoNominalVoltageRules() {
        // Given
        when(filterService.buildNominalVoltageRules(any(), any())).thenCallRealMethod();
        when(filterService.getNominalVoltageFieldType(any())).thenReturn(List.of(FieldType.NOMINAL_VOLTAGE));

        // When
        Optional<AbstractExpertRule> result = filterService.buildNominalVoltageRules(null, EquipmentType.GENERATOR);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldCreateRulesWhenNominalVoltageRulesProvided() {
        // Given
        when(filterService.buildNominalVoltageRules(any(), any())).thenCallRealMethod();
        when(filterService.getNominalVoltageFieldType(any())).thenReturn(List.of(FieldType.NOMINAL_VOLTAGE));
        when(filterService.createNumberExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.createOrCombination(any())).thenCallRealMethod();
        when(filterService.createCombination(any(), any())).thenCallRealMethod();

        List<String> voltages = Arrays.asList("400.0", "225.0");

        // When
        Optional<AbstractExpertRule> result = filterService.buildNominalVoltageRules(voltages, EquipmentType.GENERATOR);

        // Then
        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenNoCountriesCodeRules() {
        // Given
        when(filterService.buildCountryCodeRules(any(), any())).thenCallRealMethod();
        when(filterService.getCountryCodeFieldType(any())).thenReturn(List.of(FieldType.COUNTRY));

        // When
        Optional<AbstractExpertRule> result = filterService.buildCountryCodeRules(null, EquipmentType.GENERATOR);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldCreateRulesWhenCountriesCountryCodeRulesProvided() {
        // Given
        when(filterService.buildCountryCodeRules(any(), any())).thenCallRealMethod();
        when(filterService.getCountryCodeFieldType(any())).thenReturn(List.of(FieldType.COUNTRY));
        when(filterService.createEnumExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.createOrCombination(any())).thenCallRealMethod();
        when(filterService.createCombination(any(), any())).thenCallRealMethod();

        List<Country> countries = Arrays.asList(Country.FR, Country.DE);

        // When
        Optional<AbstractExpertRule> result = filterService.buildCountryCodeRules(countries, EquipmentType.GENERATOR);

        // Then
        assertTrue(result.isPresent());
    }

    @Test
    void shouldCreateRulesWhenSubstationPropertiesProvided() {
        // Given
        when(filterService.buildSubstationPropertyRules(any(), any())).thenCallRealMethod();
        when(filterService.getSubstationPropertiesFieldTypes(any())).thenReturn(List.of(FieldType.SUBSTATION_PROPERTIES));
        when(filterService.createPropertiesRule(any(), any(), any())).thenCallRealMethod();
        when(filterService.createOrCombination(any())).thenCallRealMethod();
        when(filterService.createCombination(any(), any())).thenCallRealMethod();

        Map<String, List<String>> properties = Map.of(
                "prop1", Arrays.asList("value1", "value2"),
                "prop2", List.of("value3")
        );

        // When
        Optional<AbstractExpertRule> result = filterService.buildSubstationPropertyRules(properties, EquipmentType.GENERATOR);

        // Then
        assertTrue(result.isPresent());
    }

    @Test
    void shouldReturnNullWhenNoRules() {
        // Given
        when(filterService.buildExpertFilter(any(), any())).thenCallRealMethod();
        when(filterService.buildAllExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.buildNominalVoltageRules(any(), any())).thenCallRealMethod();

        GlobalFilter globalFilter = mock(GlobalFilter.class);
        when(globalFilter.getNominalV()).thenReturn(null);
        when(globalFilter.getCountryCode()).thenReturn(null);
        when(globalFilter.getSubstationProperty()).thenReturn(null);

        // When
        ExpertFilter result = filterService.buildExpertFilter(globalFilter, EquipmentType.GENERATOR);

        // Then
        assertNull(result);
    }

    @Test
    void shouldCreateFilterWhenRulesExist() {
        // Given
        when(filterService.buildExpertFilter(any(), any())).thenCallRealMethod();
        when(filterService.buildAllExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.buildNominalVoltageRules(any(), any())).thenCallRealMethod();
        when(filterService.createCombination(any(), any())).thenCallRealMethod();
        when(filterService.getNominalVoltageFieldType(any())).thenReturn(List.of(FieldType.NOMINAL_VOLTAGE));
        when(filterService.createNumberExpertRules(any(), any())).thenCallRealMethod();
        when(filterService.createOrCombination(any())).thenCallRealMethod();

        GlobalFilter globalFilter = mock(GlobalFilter.class);
        when(globalFilter.getNominalV()).thenReturn(List.of("400.0"));
        when(globalFilter.getCountryCode()).thenReturn(null);
        when(globalFilter.getSubstationProperty()).thenReturn(null);

        // When
        ExpertFilter result = filterService.buildExpertFilter(globalFilter, EquipmentType.GENERATOR);

        // Then
        assertNotNull(result);
        assertEquals(EquipmentType.GENERATOR, result.getEquipmentType());
        assertNotNull(result.getRules());
        assertInstanceOf(CombinatorExpertRule.class, result.getRules());
    }

    @Test
    void shouldReturnFilteredNetworkWhenSameEquipmentType() {
        // Given
        when(filterService.extractEquipmentIdsFromGenericFilter(any(), any(), any())).thenCallRealMethod();
        when(filterService.filterNetwork(any(), any())).thenCallRealMethod();

        AbstractFilter filter = mock(AbstractFilter.class);
        when(filter.getEquipmentType()).thenReturn(EquipmentType.GENERATOR);

        try (MockedStatic<FilterServiceUtils> mockStatic = mockStatic(FilterServiceUtils.class)) {
            List<IdentifiableAttributes> attributes = Arrays.asList(
                    createIdentifiableAttributes("gen1"),
                    createIdentifiableAttributes("gen2")
            );
            mockStatic.when(() -> FilterServiceUtils.getIdentifiableAttributes(filter, network, filterService))
                    .thenReturn(attributes);

            // When
            List<String> result = filterService.extractEquipmentIdsFromGenericFilter(
                    filter, EquipmentType.GENERATOR, network);

            // Then
            assertEquals(Arrays.asList("gen1", "gen2"), result);
        }
    }

    @Test
    void shouldBuildVoltageLevelFilterWhenVoltageLevelType() {
        // Given
        when(filterService.extractEquipmentIdsFromGenericFilter(any(), any(), any())).thenCallRealMethod();
        when(filterService.buildExpertFilterWithVoltageLevelIdsCriteria(any(), any())).thenCallRealMethod();
        when(filterService.createVoltageLevelIdRule(any(), any())).thenCallRealMethod();
        when(filterService.createCombination(any(), any())).thenCallRealMethod();
        when(filterService.filterNetwork(any(), any())).thenCallRealMethod();

        AbstractFilter filter = mock(AbstractFilter.class);
        when(filter.getEquipmentType()).thenReturn(EquipmentType.VOLTAGE_LEVEL);
        when(filter.getId()).thenReturn(FILTER_UUID);

        try (MockedStatic<FilterServiceUtils> mockStatic = mockStatic(FilterServiceUtils.class)) {
            List<IdentifiableAttributes> attributes = Arrays.asList(
                    createIdentifiableAttributes("line1"),
                    createIdentifiableAttributes("line2")
            );
            mockStatic.when(() -> FilterServiceUtils.getIdentifiableAttributes(any(ExpertFilter.class), eq(network), eq(filterService)))
                    .thenReturn(attributes);

            // When
            List<String> result = filterService.extractEquipmentIdsFromGenericFilter(
                    filter, EquipmentType.LINE, network);

            // Then
            assertEquals(Arrays.asList("line1", "line2"), result);
        }
    }

    @Test
    void shouldReturnEmptyWhenDifferentEquipmentType() {
        // Given
        when(filterService.extractEquipmentIdsFromGenericFilter(any(), any(), any())).thenCallRealMethod();

        AbstractFilter filter = mock(AbstractFilter.class);
        when(filter.getEquipmentType()).thenReturn(EquipmentType.LOAD);

        // When
        List<String> result = filterService.extractEquipmentIdsFromGenericFilter(
                filter, EquipmentType.GENERATOR, network);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCreateExpertFilterWithVoltageLevelIdsCriteria() {
        // Given
        when(filterService.buildExpertFilterWithVoltageLevelIdsCriteria(any(), any())).thenCallRealMethod();
        when(filterService.createVoltageLevelIdRule(any(), any())).thenCallRealMethod();
        when(filterService.createCombination(any(), any())).thenCallRealMethod();

        UUID filterUuid = UUID.randomUUID();
        EquipmentType equipmentType = EquipmentType.LINE;

        // When
        ExpertFilter result = filterService.buildExpertFilterWithVoltageLevelIdsCriteria(filterUuid, equipmentType);

        // Then
        assertNotNull(result);
        assertEquals(equipmentType, result.getEquipmentType());
        assertNotNull(result.getRules());
        assertInstanceOf(CombinatorExpertRule.class, result.getRules());

        CombinatorExpertRule combinatorRule = (CombinatorExpertRule) result.getRules();
        assertEquals(CombinatorType.OR, combinatorRule.getCombinator());
        assertEquals(2, combinatorRule.getRules().size());
    }

    private IdentifiableAttributes createIdentifiableAttributes(String id) {
        IdentifiableAttributes attributes = mock(IdentifiableAttributes.class);
        when(attributes.getId()).thenReturn(id);
        return attributes;
    }
}
