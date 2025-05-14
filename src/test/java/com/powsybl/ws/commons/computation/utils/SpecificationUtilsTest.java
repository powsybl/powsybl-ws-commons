/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.computation.utils;

import com.powsybl.security.LimitViolation;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.powsybl.ws.commons.computation.utils.SpecificationUtils.getColumnPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class SpecificationUtilsTest {

    @Test
    void testGetColumnPath() {
        Root root = Mockito.mock(Root.class);

        Path<Object> subjectIdPath = Mockito.mock(Path.class);
        when(root.get("subjectId")).thenReturn(subjectIdPath);

        Path<LimitViolation> path = getColumnPath(root, "subjectId");

        assertEquals(subjectIdPath, path);
    }

    @Test
    void testGetDotSeparatedColumnPath() {
        Root root = Mockito.mock(Root.class);
        Root rootVoltageLocation = Mockito.mock(Root.class);

        Path voltageLocationTypePath = Mockito.mock(Path.class);
        when(rootVoltageLocation.get("type")).thenReturn(voltageLocationTypePath);
        when(root.get("voltageLocation")).thenReturn(rootVoltageLocation);

        Path path = getColumnPath(root, "voltageLocation.type");
        assertEquals(voltageLocationTypePath, path);
    }
}
