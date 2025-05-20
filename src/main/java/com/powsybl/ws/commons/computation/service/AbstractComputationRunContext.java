/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.service;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.ws.commons.computation.dto.ReportInfos;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france.com>
 * @param <P> parameters structure specific to the computation
 */
@Getter
@Setter
public abstract class AbstractComputationRunContext<P> {
    private final UUID networkUuid;
    private final String variantId;
    private final String receiver;
    private final ReportInfos reportInfos;
    private final String userId;
    private String provider;
    private P parameters;
    private ReportNode reportNode;
    private Network network;
    private Boolean debug;
    private ComputationManager computationManager;

    protected AbstractComputationRunContext(UUID networkUuid, String variantId, String receiver, ReportInfos reportInfos,
                                            String userId, String provider, P parameters) {
        this(networkUuid, variantId, receiver, reportInfos, userId, provider, parameters, null);
    }

    protected AbstractComputationRunContext(UUID networkUuid, String variantId, String receiver, ReportInfos reportInfos,
                                            String userId, String provider, P parameters, Boolean debug) {
        this.networkUuid = networkUuid;
        this.variantId = variantId;
        this.receiver = receiver;
        this.reportInfos = reportInfos;
        this.userId = userId;
        this.provider = provider;
        this.parameters = parameters;
        this.reportNode = ReportNode.NO_OP;
        this.network = null;
        this.debug = debug;
    }
}
