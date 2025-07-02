/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.ws.commons;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class SecuredInputStream {
    private final int maxEntries;
    private final long maxSize;
    private int entryCount = 0;
    private long totalReadBytes = 0;

    protected SecuredInputStream(int maxEntries, long maxSize) {
        super();
        this.maxEntries = maxEntries;
        this.maxSize = maxSize;
    }

    public void incrementAndValidateEntryLimit() {
        if (++entryCount > maxEntries) {
            throw new IllegalStateException("Archive has too many entries.");
        }
    }

    public void checkBeforeRead(int len) {
        if (len + totalReadBytes > maxSize) {
            throw new IllegalStateException("Archive size is too big.");
        }
    }

    public void incrementAndValidateMaxSize(int readBytes) {
        totalReadBytes += readBytes;
        if (totalReadBytes > maxSize) {
            throw new IllegalStateException("Archive size is too big.");
        }
    }
}