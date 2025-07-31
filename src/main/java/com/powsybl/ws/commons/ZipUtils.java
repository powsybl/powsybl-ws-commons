/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ZipUtils {
    private ZipUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static void zip(Path sourceDirPath, Path outputZipFilePath) {
        if (!Files.isDirectory(sourceDirPath)) {
            throw new IllegalArgumentException("Provided path is not a directory.");
        }
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(outputZipFilePath));
            Stream<Path> pathStream = Files.walk(sourceDirPath)) {
            pathStream
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                    try {
                        zs.putNextEntry(zipEntry);
                        Files.copy(path, zs);
                        zs.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException("Error occurred while zipping the file: " + path, e);
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException("Error occurred while zipping the directory: " + sourceDirPath, e);
        }
    }
}
