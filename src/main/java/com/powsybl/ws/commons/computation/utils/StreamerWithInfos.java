/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.computation.utils;

import lombok.Builder;
import lombok.Getter;

import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Builder
@Getter
public class StreamerWithInfos {
    Consumer<OutputStream> streamer;
    String fileName;
    Long fileLength;
}
