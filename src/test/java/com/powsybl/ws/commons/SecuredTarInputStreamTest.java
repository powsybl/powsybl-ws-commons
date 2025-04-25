/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
class SecuredTarInputStreamTest {

    @Test
    void test() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.tar");
        Objects.requireNonNull(inputStream);
        BufferedInputStream tarStream = new BufferedInputStream(inputStream);
        try (SecuredTarInputStream tooManyEntriesSecuredTis = new SecuredTarInputStream(tarStream, 3, 1000000000)) {
            assertTrue(assertThrows(IllegalStateException.class, () -> readTar(tooManyEntriesSecuredTis))
                .getMessage().contains("Archive has too many entries."));
        }

        // must reload or the inpustream is closing
        inputStream = getClass().getResourceAsStream("/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.tar");
        Objects.requireNonNull(inputStream);
        tarStream = new BufferedInputStream(inputStream);
        try (SecuredTarInputStream tooBigSecuredTis = new SecuredTarInputStream(tarStream, 1000, 15000)) {
            assertTrue(assertThrows(IllegalStateException.class, () -> readTar(tooBigSecuredTis))
                .getMessage().contains("Archive size is too big."));
        }

        inputStream = getClass().getResourceAsStream("/MicroGridTestConfiguration_T4_BE_BB_Complete_v2.tar");
        Objects.requireNonNull(inputStream);
        tarStream = new BufferedInputStream(inputStream);
        try (SecuredTarInputStream okSecuredTis = new SecuredTarInputStream(tarStream, 1000, 1000000000)) {
            TarArchiveInputStream tis = new TarArchiveInputStream(tarStream);
            readTar(tis);
            assertEquals(readTar(tis), readTar(okSecuredTis));
        }
    }

    private static int readTar(TarArchiveInputStream tarArchiveInputStream) throws IOException {
        TarArchiveEntry entry = tarArchiveInputStream.getNextEntry();
        int readBytes = 0;
        while (entry != null) {
            readBytes += tarArchiveInputStream.readAllBytes().length;
            entry = tarArchiveInputStream.getNextEntry();
        }
        return readBytes;
    }
}
