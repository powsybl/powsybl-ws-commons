/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons.computation.service;

import java.util.List;
import java.util.UUID;

/**
 * @author Mathieu Deharbe <mathieu.deharbe at rte-france.com>
 * @param <S> status specific to the computation
 */
public abstract class AbstractComputationResultService<S> {

    public abstract void insertStatus(List<UUID> resultUuids, S status);

    public abstract void delete(UUID resultUuid);

    public abstract void deleteAll();

    public abstract S findStatus(UUID resultUuid);

    // --- Must implement these following methods if a computation server supports s3 storage --- //
    public void saveDebugFileLocation(UUID resultUuid, String debugFilePath) {
        // to override by subclasses
    }

    public String findDebugFileLocation(UUID resultUuid) {
        // to override by subclasses
        return null;
    }
}
