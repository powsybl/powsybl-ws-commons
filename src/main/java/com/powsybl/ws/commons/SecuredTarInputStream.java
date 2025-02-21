/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class SecuredTarInputStream extends TarArchiveInputStream {
    private final int maxZipEntries;
    private final long maxSize;
    private int entryCount = 0;
    private int totalReadBytes = 0;

    public SecuredTarInputStream(InputStream in, int maxZipEntries, long maxSize) {
        super(in);
        this.maxZipEntries = maxZipEntries;
        this.maxSize = maxSize;
    }

    @Override
    public TarArchiveEntry getNextEntry() throws IOException {
        if (++entryCount > maxZipEntries) {
            throw new IllegalStateException("Tar has too many entries.");
        }
        return super.getNextEntry();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len + totalReadBytes > maxSize) {
            throw new IllegalStateException("Tar size is too big.");
        }

        int readBytes = super.read(b, off, len);

        totalReadBytes += readBytes;
        if (totalReadBytes > maxSize) {
            throw new IllegalStateException("Tar size is too big.");
        }

        return readBytes;
    }
}
