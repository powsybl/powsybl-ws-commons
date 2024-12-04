package com.powsybl.ws.commons.computation.utils;

import com.powsybl.iidm.network.*;
import com.powsybl.security.BusBreakerViolationLocation;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.NodeBreakerViolationLocation;
import com.powsybl.security.ViolationLocation;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.iidm.network.IdentifiableType.BUSBAR_SECTION;
import static com.powsybl.security.ViolationLocation.Type.NODE_BREAKER;

public final class ComputationResultUtils {

    private ComputationResultUtils() {
    }

    public static String getViolationLocationId(LimitViolation limitViolation, Network network) {
        Optional<ViolationLocation> violationLocation = limitViolation.getViolationLocation();
        if (violationLocation.isEmpty()) {
            return limitViolation.getSubjectId();
        }

        ViolationLocation location = violationLocation.get();
        if (location.getType() == NODE_BREAKER) {
            return getNodeBreakerViolationLocationId((NodeBreakerViolationLocation) location, network);
        } else {
            return getBusBreakerViolationLocationId((BusBreakerViolationLocation) location, network, limitViolation.getSubjectId());
        }
    }

    private static String getNodeBreakerViolationLocationId(NodeBreakerViolationLocation nodeBreakerViolationLocation, Network network) {
        VoltageLevel vl = network.getVoltageLevel(nodeBreakerViolationLocation.getVoltageLevelId());

        Set<String> busBarIds = nodeBreakerViolationLocation.getNodes().stream()
                .map(node -> vl.getNodeBreakerView().getTerminal(node))
                .filter(Objects::nonNull)
                .map(Terminal::getConnectable)
                .filter(t -> t.getType() == BUSBAR_SECTION)
                .map(Identifiable::getId)
                .collect(Collectors.toSet());

        String busId = null;
        if (busBarIds.isEmpty()) {
            busId = getBusId(vl, busBarIds);
        }
        return formatViolationLocationId(busId != null ? Set.of(busId) : busBarIds, nodeBreakerViolationLocation.getVoltageLevelId());
    }

    private static String getBusId(VoltageLevel voltageLevel, Set<String> sjbIds) {
        Optional<Bus> bus = voltageLevel.getBusView()
            .getBusStream()
            .filter(b -> {
                Set<String> busSjbIds = b.getConnectedTerminalStream().map(Terminal::getConnectable).filter(c -> c.getType() == BUSBAR_SECTION).map(Connectable::getId).collect(Collectors.toSet());
                return busSjbIds.equals(sjbIds);
            })
            .findFirst();
        return bus.isPresent() ? bus.get().getId() : null;
    }

    private static String formatViolationLocationId(Set<String> elementsIds, String subjectId) {
        if (elementsIds.size() == 1) {
            return elementsIds.stream().findFirst().get();
        } else if (elementsIds.isEmpty()) {
            return subjectId;
        } else {
            return subjectId + " (" + String.join(", ", elementsIds) + " )";
        }
    }

    private static String getBusBreakerViolationLocationId(BusBreakerViolationLocation busBreakerViolationLocation, Network network, String subjectId) {
        Set<String> busBreakerIds = busBreakerViolationLocation
                .getBusView(network)
                .getBusStream()
                .map(Identifiable::getId)
                .collect(Collectors.toSet());

        return formatViolationLocationId(busBreakerIds, subjectId);
    }

}
