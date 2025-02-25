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
    private final SecuredInputStream securedStream;

    public SecuredTarInputStream(InputStream in, int maxTarEntries, long maxSize) {
        super(in);
        this.securedStream = new SecuredInputStream(maxTarEntries, maxSize);
    }

    @Override
    public TarArchiveEntry getNextEntry() throws IOException {
        securedStream.incrementAndValidateEntryLimit();
        return super.getNextEntry();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        securedStream.incrementAndValidateMaxSize(len);
        return super.read(b, off, len);
    }
}
