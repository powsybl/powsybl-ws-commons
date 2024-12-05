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

    static Network createBusBreakerNetwork() {
        Network network = new NetworkFactoryImpl().createNetwork("network", "test");
        Substation p1 = network.newSubstation()
                .setId("P1")
                .setCountry(Country.FR)
                .setTso("RTE")
                .setGeographicalTags("A")
                .add();
        VoltageLevel vl = p1.newVoltageLevel()
                .setId("VLGEN")
                .setNominalV(24.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus ngen = vl.getBusBreakerView().newBus()
                .setId("NGEN")
                .add();

        vl.getBusBreakerView().newBus()
            .setId("NGEN2")
            .add();

        vl.newLoad()
            .setId("LD")
            .setBus(ngen.getId())
            .setConnectableBus(ngen.getId())
            .setP0(600.0)
            .setQ0(200.0)
            .add();

        return network;
    }

    public static Network createNodeBreakerNetwork() {
        Network network = Network.create("network", "test");
        Substation s = network.newSubstation()
                .setId("S")
                .add();
        VoltageLevel vl = s.newVoltageLevel()
                .setId("VL1")
                .setNominalV(400)
                .setLowVoltageLimit(370.)
                .setHighVoltageLimit(420.)
                .setTopologyKind(TopologyKind.NODE_BREAKER)
                .add();
        vl.getNodeBreakerView().newBusbarSection()
                .setId("BBS1")
                .setNode(0)
                .add();
        vl.getNodeBreakerView().newBusbarSection()
                .setId("BBS2")
                .setNode(1)
                .add();

        vl.newLoad()
            .setId("LD")
            .setNode(3)
            .setP0(600.0)
            .setQ0(200.0)
            .add();

        vl.getNodeBreakerView().newBreaker()
            .setId("BR")
            .setOpen(false)
            .setNode1(0)
            .setNode2(3)
            .add();

        return network;
    }

    @Test
    void testViolationLocationIdBusBreaker() {
        Network network = createBusBreakerNetwork();

        LimitViolation limitViolation = mock(LimitViolation.class);

        when(limitViolation.getViolationLocation()).thenReturn(Optional.of(new BusBreakerViolationLocation(List.of("NGEN"))));
        assertEquals("VLGEN_0", ComputationResultUtils.getViolationLocationId(limitViolation, network));

        when(limitViolation.getViolationLocation()).thenReturn(Optional.of(new BusBreakerViolationLocation(List.of("NGEN2"))));
        when(limitViolation.getSubjectId()).thenReturn("VLGEN");
        assertEquals("VLGEN", ComputationResultUtils.getViolationLocationId(limitViolation, network));

        VoltageLevel vl = network.getVoltageLevel("VLGEN");
        vl.newLoad()
                .setId("LD2")
                .setBus("NGEN2")
                .setConnectableBus("NGEN2")
                .setP0(600.0)
                .setQ0(200.0)
                .add();
        when(limitViolation.getViolationLocation()).thenReturn(Optional.of(new BusBreakerViolationLocation(List.of("NGEN", "NGEN2"))));
        assertEquals("VLGEN (VLGEN_0, VLGEN_1)", ComputationResultUtils.getViolationLocationId(limitViolation, network));

    }

    @Test
    void testNoViolationLocationIdBusBreaker() {
        Network network = createBusBreakerNetwork();
        LimitViolation limitViolation = mock(LimitViolation.class);

        when(limitViolation.getViolationLocation()).thenReturn(Optional.empty());
        when(limitViolation.getSubjectId()).thenReturn("subjectId");
        assertEquals("subjectId", ComputationResultUtils.getViolationLocationId(limitViolation, network));
    }

    @Test
    void testViolationLocationIdNodeBreaker() {
        Network network = createNodeBreakerNetwork();

        NodeBreakerViolationLocation nodeBreakerViolationLocation = mock(NodeBreakerViolationLocation.class);
        when(nodeBreakerViolationLocation.getType()).thenReturn(ViolationLocation.Type.NODE_BREAKER);
        when(nodeBreakerViolationLocation.getVoltageLevelId()).thenReturn("VL1");
        when(nodeBreakerViolationLocation.getNodes()).thenReturn(List.of());

        LimitViolation limitViolation = mock(LimitViolation.class);
        when(limitViolation.getViolationLocation()).thenReturn(Optional.of(nodeBreakerViolationLocation));
        when(limitViolation.getSubjectId()).thenReturn("VLHV1");

        String locationId = ComputationResultUtils.getViolationLocationId(limitViolation, network);
        assertEquals("VL1", locationId);

        when(nodeBreakerViolationLocation.getNodes()).thenReturn(List.of(0, 1));
        locationId = ComputationResultUtils.getViolationLocationId(limitViolation, network);
        assertEquals("VL1 (BBS1, BBS2)", locationId);

        when(nodeBreakerViolationLocation.getNodes()).thenReturn(List.of(0));
        locationId = ComputationResultUtils.getViolationLocationId(limitViolation, network);
        assertEquals("VL1_0", locationId);

        when(nodeBreakerViolationLocation.getNodes()).thenReturn(List.of(1));
        locationId = ComputationResultUtils.getViolationLocationId(limitViolation, network);
        assertEquals("VL1 (BBS2)", locationId);
    }
}
