package com.powsybl.ws.commons.computation;

import com.powsybl.iidm.network.*;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import com.powsybl.security.BusBreakerViolationLocation;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.NodeBreakerViolationLocation;
import com.powsybl.security.ViolationLocation;
import com.powsybl.ws.commons.computation.utils.ComputationResultUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComputationUtilTest {

    static final String VARIANT_1_ID = "variant_1";

    static Network createNetwork(String prefix, boolean createVariant) {
        Network network = new NetworkFactoryImpl().createNetwork(prefix + "network", "test");
        Substation p1 = network.newSubstation()
                .setId(prefix + "P1")
                .setCountry(Country.FR)
                .setTso("RTE")
                .setGeographicalTags("A")
                .add();
        VoltageLevel vlgen = p1.newVoltageLevel()
                .setId(prefix + "VLGEN")
                .setNominalV(24.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        VoltageLevel vlhv1 = p1.newVoltageLevel()
                .setId(prefix + "VLHV1")
                .setNominalV(380.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus ngen = vlgen.getBusBreakerView().newBus()
                .setId(prefix + "NGEN")
                .add();
        Bus nhv1 = vlhv1.getBusBreakerView().newBus()
                .setId(prefix + "NHV1")
                .add();

        int zb380 = 380 * 380 / 100;
        p1.newTwoWindingsTransformer()
                .setId(prefix + "NGEN_NHV1")
                .setVoltageLevel1(vlgen.getId())
                .setBus1(ngen.getId())
                .setConnectableBus1(ngen.getId())
                .setRatedU1(24.0)
                .setVoltageLevel2(vlhv1.getId())
                .setBus2(nhv1.getId())
                .setConnectableBus2(nhv1.getId())
                .setRatedU2(400.0)
                .setR(0.24 / 1300 * zb380)
                .setX(Math.sqrt(10 * 10 - 0.24 * 0.24) / 1300 * zb380)
                .setG(0.0)
                .setB(0.0)
                .add();

        if (createVariant) {
            network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        }

        return network;
    }

    public static Network createNodeBreakerNetwork() {
        Network network = Network.create("test", "test");
        Substation s = network.newSubstation()
                .setId("S")
                .add();
        VoltageLevel vl1 = s.newVoltageLevel()
                .setId("VL1")
                .setNominalV(400)
                .setLowVoltageLimit(370.)
                .setHighVoltageLimit(420.)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .add();
        vl1.getNodeBreakerView().newBusbarSection()
                .setId("BBS1")
                .setNode(0)
                .add();
        vl1.getNodeBreakerView().newBusbarSection()
                .setId("BBS2")
                .setNode(1)
                .add();

        VoltageLevel vl2 = s.newVoltageLevel()
                .setId("VL2")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .setLowVoltageLimit(370.)
                .setHighVoltageLimit(420.)
                .add();
        vl2.getNodeBreakerView().newBusbarSection()
                .setId("BBS3")
                .setNode(0)
                .add();

        return network;
    }

    @Test
    void testGetIdFromViolationWithBusBreaker() {
        // Setup
        Network network = createNetwork("", true);
        BusBreakerViolationLocation busBreakerLocation = new BusBreakerViolationLocation(List.of("NGEN"));
        LimitViolation limitViolation = mock(LimitViolation.class);
        when(limitViolation.getViolationLocation()).thenReturn(Optional.of(busBreakerLocation));

        assertEquals("VLGEN_0", ComputationResultUtils.getViolationLocationId(limitViolation, network));
    }

    @Test
    void testGetIdFromViolationWithNoViolationLocation() {
        // Setup
        Network network = mock(Network.class);
        LimitViolation limitViolation = mock(LimitViolation.class);

        when(limitViolation.getViolationLocation()).thenReturn(Optional.empty());
        when(limitViolation.getSubjectId()).thenReturn("SubjectId");

        assertEquals("SubjectId", ComputationResultUtils.getViolationLocationId(limitViolation, network));
    }

    @Test
    void testGetIdFromViolationWithNodeBreaker() {
        // Create a real network instead of mocking it
        Network network = createNodeBreakerNetwork();

        NodeBreakerViolationLocation nodeBreakerViolationLocation = mock(NodeBreakerViolationLocation.class);
        when(nodeBreakerViolationLocation.getType()).thenReturn(ViolationLocation.Type.NODE_BREAKER);
        when(nodeBreakerViolationLocation.getVoltageLevelId()).thenReturn("VL1");

        LimitViolation limitViolation = mock(LimitViolation.class);
        when(limitViolation.getViolationLocation()).thenReturn(Optional.of(nodeBreakerViolationLocation));
        when(limitViolation.getSubjectId()).thenReturn("VLHV1");

        String result = ComputationResultUtils.getViolationLocationId(limitViolation, network);
        assertEquals("VL1", result);
    }
}
