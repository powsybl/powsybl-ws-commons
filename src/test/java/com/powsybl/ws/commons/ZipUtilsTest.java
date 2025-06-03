/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
class ZipUtilsTest {

    @Test
    void testZipWithValidDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        Path sourceDir = tempDir.resolve("sourceDir");
        Files.createDirectories(sourceDir);

        Path file1 = sourceDir.resolve("file1.txt");
        Path file2 = sourceDir.resolve("file2.txt");
        Files.writeString(file1, "content 1");
        Files.writeString(file2, "content 2");

        Path zipFile = tempDir.resolve("sourceDir.zip");

        // perform test
        ZipUtils.zip(sourceDir, zipFile);

        // check file existence
        assertThat(zipFile).exists().isReadable();

        // extract file names in zip file to check later
        Set<String> entryNames = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }

        // check file names
        assertThat(entryNames)
                .hasSize(2)
                .containsExactlyInAnyOrder("file1.txt", "file2.txt");
    }

    @Test
    void testZipThrowsExceptionWhenSourceIsNotDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        Path aFile = tempDir.resolve("file.txt");
        Files.writeString(aFile, "content");

        Path zipFile = tempDir.resolve("aFile.zip");

        // perform test and check
        assertThatThrownBy(() -> ZipUtils.zip(aFile, zipFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provided path is not a directory.");
    }
}

