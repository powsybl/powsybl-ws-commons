/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
public class SecuredZipInputStream extends ZipInputStream {

    private final SecuredInputStream securedStream;

    public SecuredZipInputStream(InputStream in, int maxZipEntries, long maxSize) {
        this(in, StandardCharsets.UTF_8, maxZipEntries, maxSize);
    }

    public SecuredZipInputStream(InputStream in, Charset charset, int maxZipEntries, long maxSize) {
        super(in, charset);
        securedStream = new SecuredInputStream(maxZipEntries, maxSize);
    }

    @Override
    public ZipEntry getNextEntry() throws IOException {
        securedStream.incrementAndValidateEntryLimit();
        return super.getNextEntry();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        securedStream.checkBeforeRead(len);
        int readBytes = super.read(b, off, len);
        securedStream.incrementAndValidateMaxSize(len, readBytes);
        return readBytes;
    }
}
