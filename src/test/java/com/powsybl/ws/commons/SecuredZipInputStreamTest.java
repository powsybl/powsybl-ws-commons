/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
class SecuredZipInputStreamTest {
    @Test
    void test() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.zip");
        Objects.requireNonNull(inputStream);
        byte[] fileContent = ByteStreams.toByteArray(inputStream);
        try (SecuredZipInputStream tooManyEntriesSecuredZis = new SecuredZipInputStream(new ByteArrayInputStream(fileContent), 3, 1000000000)) {
            assertTrue(assertThrows(IllegalStateException.class, () -> readZip(tooManyEntriesSecuredZis))
                    .getMessage().contains("Archive has too many entries."));
        }

        try (SecuredZipInputStream tooBigSecuredZis = new SecuredZipInputStream(new ByteArrayInputStream(fileContent), 1000, 15000)) {
            assertTrue(assertThrows(IllegalStateException.class, () -> readZip(tooBigSecuredZis))
                    .getMessage().contains("Archive size is too big."));
        }

        try (SecuredZipInputStream okSecuredZis = new SecuredZipInputStream(new ByteArrayInputStream(fileContent), 1000, 1000000000)) {
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileContent));
            assertEquals(readZip(zis), readZip(okSecuredZis));
        }
    }

    private static int readZip(ZipInputStream zis) throws IOException {
        ZipEntry entry = zis.getNextEntry();
        int readBytes = 0;
        while (entry != null) {
            readBytes += zis.readAllBytes().length;
            entry = zis.getNextEntry();
        }
        return readBytes;
    }
}
