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
        Substation p2 = network.newSubstation()
                .setId(prefix + "P2")
                .setCountry(Country.FR)
                .setTso("RTE")
                .setGeographicalTags("B")
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
        VoltageLevel vlhv2 = p2.newVoltageLevel()
                .setId(prefix + "VLHV2")
                .setNominalV(380.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        VoltageLevel vlload = p2.newVoltageLevel()
                .setId(prefix + "VLLOAD")
                .setNominalV(150.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus ngen = vlgen.getBusBreakerView().newBus()
                .setId(prefix + "NGEN")
                .add();
        Bus nhv1 = vlhv1.getBusBreakerView().newBus()
                .setId(prefix + "NHV1")
                .add();
        Bus nhv2 = vlhv2.getBusBreakerView().newBus()
                .setId(prefix + "NHV2")
                .add();
        Bus nload = vlload.getBusBreakerView().newBus()
                .setId(prefix + "NLOAD")
                .add();
        network.newLine()
                .setId(prefix + "NHV1_NHV2_1")
                .setVoltageLevel1(vlhv1.getId())
                .setBus1(nhv1.getId())
                .setConnectableBus1(nhv1.getId())
                .setVoltageLevel2(vlhv2.getId())
                .setBus2(nhv2.getId())
                .setConnectableBus2(nhv2.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();
        network.newLine()
                .setId(prefix + "NHV1_NHV2_2")
                .setVoltageLevel1(vlhv1.getId())
                .setBus1(nhv1.getId())
                .setConnectableBus1(nhv1.getId())
                .setVoltageLevel2(vlhv2.getId())
                .setBus2(nhv2.getId())
                .setConnectableBus2(nhv2.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
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
        int zb150 = 150 * 150 / 100;
        TwoWindingsTransformer nhv2Nload = p2.newTwoWindingsTransformer()
                .setId(prefix + "NHV2_NLOAD")
                .setVoltageLevel1(vlhv2.getId())
                .setBus1(nhv2.getId())
                .setConnectableBus1(nhv2.getId())
                .setRatedU1(400.0)
                .setVoltageLevel2(vlload.getId())
                .setBus2(nload.getId())
                .setConnectableBus2(nload.getId())
                .setRatedU2(158.0)
                .setR(0.21 / 1000 * zb150)
                .setX(Math.sqrt(18 * 18 - 0.21 * 0.21) / 1000 * zb150)
                .setG(0.0)
                .setB(0.0)
                .add();
        double a = (158.0 / 150.0) / (400.0 / 380.0);
        nhv2Nload.newRatioTapChanger()
                .beginStep()
                .setRho(0.85f * a)
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep()
                .beginStep()
                .setRho(a)
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep()
                .beginStep()
                .setRho(1.15f * a)
                .setR(0.0)
                .setX(0.0)
                .setG(0.0)
                .setB(0.0)
                .endStep()
                .setTapPosition(1)
                .setLoadTapChangingCapabilities(true)
                .setRegulating(true)
                .setTargetV(158.0)
                .setTargetDeadband(0)
                .setRegulationTerminal(nhv2Nload.getTerminal2())
                .add();
        vlload.newLoad()
                .setId(prefix + "LOAD")
                .setBus(nload.getId())
                .setConnectableBus(nload.getId())
                .setP0(600.0)
                .setQ0(200.0)
                .add();
        Generator generator = vlgen.newGenerator()
                .setId(prefix + "GEN")
                .setBus(ngen.getId())
                .setConnectableBus(ngen.getId())
                .setMinP(-9999.99)
                .setMaxP(9999.99)
                .setVoltageRegulatorOn(true)
                .setTargetV(24.5)
                .setTargetP(607.0)
                .setTargetQ(301.0)
                .add();
        generator.newMinMaxReactiveLimits()
                .setMinQ(-9999.99)
                .setMaxQ(9999.99)
                .add();

        if (createVariant) {
            network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        }

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

//    @Test
//    void testGetIdFromViolationWithNodeBreaker() {
//        // Create a real network instead of mocking it
//        Network network = createNetwork("testNetwork", true);
//
//        NodeBreakerViolationLocation nodeBreakerViolationLocation = mock(NodeBreakerViolationLocation.class);
//        when(nodeBreakerViolationLocation.getType()).thenReturn(ViolationLocation.Type.NODE_BREAKER);
//        when(nodeBreakerViolationLocation.getVoltageLevelId()).thenReturn("VLHV1");
//
//        LimitViolation limitViolation = mock(LimitViolation.class);
//        when(limitViolation.getViolationLocation()).thenReturn(Optional.of(nodeBreakerViolationLocation));
//        when(limitViolation.getSubjectId()).thenReturn("VLHV1");
//
//        String result = ComputationResultUtils.getViolationLocationId(limitViolation, network);
//        assertEquals("VL1", result);
//    }
}
