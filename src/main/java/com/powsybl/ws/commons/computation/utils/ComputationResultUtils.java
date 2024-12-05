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

        List<String> busBarIds = nodeBreakerViolationLocation.getNodes().stream()
                .map(node -> vl.getNodeBreakerView().getTerminal(node))
                .filter(Objects::nonNull)
                .map(Terminal::getConnectable)
                .filter(t -> t.getType() == BUSBAR_SECTION)
                .map(Identifiable::getId)
                .distinct()
                .toList();

        String busId = null;
        if (!busBarIds.isEmpty()) {
            busId = getBusId(vl, new HashSet<>(busBarIds));
        }
        return formatViolationLocationId(busId != null ? List.of() : busBarIds, busId != null ? busId : nodeBreakerViolationLocation.getVoltageLevelId());
    }

    private static String getBusId(VoltageLevel voltageLevel, Set<String> sjbIds) {
        Optional<Bus> bus = voltageLevel.getBusView()
            .getBusStream()
            .filter(b -> {
                Set<String> busSjbIds = b.getConnectedTerminalStream().map(Terminal::getConnectable).filter(c -> c.getType() == BUSBAR_SECTION).map(Connectable::getId).collect(Collectors.toSet());
                return busSjbIds.equals(sjbIds);
            })
            .findFirst();
        return bus.map(Identifiable::getId).orElse(null);
    }

    private static String formatViolationLocationId(List<String> elementsIds, String subjectId) {
        return !elementsIds.isEmpty() ?
            subjectId + " (" + String.join(", ", elementsIds) + ")" :
            subjectId;
    }

    private static String getBusBreakerViolationLocationId(BusBreakerViolationLocation busBreakerViolationLocation, Network network, String subjectId) {
        List<String> busBreakerIds = busBreakerViolationLocation
                .getBusView(network)
                .getBusStream()
                .map(Identifiable::getId)
                .distinct()
                .toList();

        return busBreakerIds.size() == 1 ? formatViolationLocationId(List.of(), busBreakerIds.get(0)) : formatViolationLocationId(busBreakerIds, subjectId);
    }

}