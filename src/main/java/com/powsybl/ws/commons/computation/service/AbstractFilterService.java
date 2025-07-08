package com.powsybl.ws.commons.computation.service;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import io.micrometer.common.util.StringUtils;
import org.gridsuite.filter.AbstractFilter;
import org.gridsuite.filter.expertfilter.expertrule.*;
import org.gridsuite.filter.identifierlistfilter.IdentifiableAttributes;
import org.gridsuite.filter.utils.EquipmentType;
import org.gridsuite.filter.utils.FilterServiceUtils;
import org.gridsuite.filter.utils.expertfilter.CombinatorType;
import org.gridsuite.filter.utils.expertfilter.FieldType;
import org.gridsuite.filter.utils.expertfilter.OperatorType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Rehili Ghazwa <ghazwa.rehili at rte-france.com>
 */
@Service
public abstract class AbstractFilterService implements FilterService {
    protected static final String FILTERS_NOT_FOUND = "Filters not found";
    protected static final String FILTER_API_VERSION = "v1";
    protected static final String DELIMITER = "/";

    protected final RestTemplate restTemplate = new RestTemplate();
    protected final NetworkStoreService networkStoreService;
    protected final String filterServerBaseUri;
    public static final String NETWORK_UUID = "networkUuid";

    public static final String IDS = "ids";

    protected AbstractFilterService(NetworkStoreService networkStoreService, String filterServerBaseUri) {
        this.networkStoreService = networkStoreService;
        this.filterServerBaseUri = filterServerBaseUri;
    }

    @Override
    public List<AbstractFilter> getFilters(List<UUID> filtersUuids) {
        if (CollectionUtils.isEmpty(filtersUuids)) {
            return List.of();
        }

        String ids = filtersUuids.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        String path = UriComponentsBuilder
                .fromPath(DELIMITER + FILTER_API_VERSION + "/filters/metadata")
                .queryParam("ids", ids)
                .buildAndExpand()
                .toUriString();

        try {
            return restTemplate.exchange(
                    filterServerBaseUri + path,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<AbstractFilter>>() { }
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new PowsyblException(FILTERS_NOT_FOUND + " [" + filtersUuids + "]");
        }
    }

    protected Network getNetwork(UUID networkUuid, String variantId) {
        try {
            Network network = networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION);
            if (network == null) {
                throw new PowsyblException("Network '" + networkUuid + "' not found");
            }
            if (StringUtils.isNotBlank(variantId)) {
                network.getVariantManager().setWorkingVariant(variantId);
            }
            return network;
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    protected List<String> filterNetwork(AbstractFilter filter, Network network) {
        return FilterServiceUtils.getIdentifiableAttributes(filter, network, this)
                .stream()
                .map(IdentifiableAttributes::getId)
                .collect(Collectors.toList());
    }

    protected List<AbstractExpertRule> createNumberExpertRules(List<String> values, FieldType fieldType) {
        return values == null ? List.of() :
                values.stream()
                        .map(value -> NumberExpertRule.builder()
                                .value(Double.valueOf(value))
                                .field(fieldType)
                                .operator(OperatorType.EQUALS)
                                .build())
                        .collect(Collectors.toList());
    }

    protected AbstractExpertRule createPropertiesRule(String property, List<String> propertiesValues, FieldType fieldType) {
        return PropertiesExpertRule.builder()
                .combinator(CombinatorType.OR)
                .operator(OperatorType.IN)
                .field(fieldType)
                .propertyName(property)
                .propertyValues(propertiesValues)
                .build();
    }

    protected List<AbstractExpertRule> createEnumExpertRules(List<Country> values, FieldType fieldType) {
        return values == null ? List.of() :
                values.stream()
                        .map(value -> EnumExpertRule.builder()
                                .value(value.toString())
                                .field(fieldType)
                                .operator(OperatorType.EQUALS)
                                .build())
                        .collect(Collectors.toList());
    }

    protected AbstractExpertRule createCombination(CombinatorType combinatorType, List<AbstractExpertRule> rules) {
        return CombinatorExpertRule.builder()
                .combinator(combinatorType)
                .rules(rules)
                .build();
    }

    protected Optional<AbstractExpertRule> createOrCombination(List<AbstractExpertRule> rules) {
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rules.size() > 1 ?
                createCombination(CombinatorType.OR, rules) :
                rules.getFirst());
    }

    protected abstract List<FieldType> getNominalVoltageFieldType(EquipmentType equipmentType);

    protected abstract List<FieldType> getCountryCodeFieldType(EquipmentType equipmentType);

    protected abstract List<FieldType> getSubstationPropertiesFieldTypes(EquipmentType equipmentType);
}



