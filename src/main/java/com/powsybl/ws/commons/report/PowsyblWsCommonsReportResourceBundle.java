/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.ws.commons.report;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportResourceBundle;

/**
 * @author Alice Caron {@literal <alice.caron at rte-france.com>}
 */
@AutoService(ReportResourceBundle.class)
public final class PowsyblWsCommonsReportResourceBundle implements ReportResourceBundle {

    public static final String BASE_NAME = "com.powsybl.ws.commons.reports";

    public String getBaseName() {
        return BASE_NAME;
    }
}
