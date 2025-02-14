/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class GZipUtils {

    private GZipUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static byte[] zip(InputStream is) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             GZIPOutputStream zipOs = new GZIPOutputStream(os)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                zipOs.write(buffer, 0, length);
            }
            zipOs.finish();
            return os.toByteArray();
        }
    }

    public static byte[] zip(String content) throws IOException {
        try (InputStream is = new ByteArrayInputStream(content.getBytes())) {
            return zip(is);
        }
    }

    public static byte[] zip(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            return zip(is);
        } catch (IOException e) {
            throw new UncheckedIOException("Error occurred while zipping the file " + filePath.toAbsolutePath(), e);
        }
    }

    private static void unzipToStream(byte[] zippedBytes, OutputStream outputStream) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(zippedBytes);
             GZIPInputStream zipIs = new GZIPInputStream(is);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(outputStream)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zipIs.read(buffer)) > 0) {
                bufferedOut.write(buffer, 0, length);
            }
        }
    }

    public static void unzip(byte[] zippedBytes, Path filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(filePath.toUri()))) {
            unzipToStream(zippedBytes, fos);
        }
    }

    public static <T> T unzip(byte[] zippedBytes, ObjectMapper objectMapper, TypeReference<T> valueTypeRef) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            unzipToStream(zippedBytes, bos);
            return objectMapper.readValue(bos.toByteArray(), valueTypeRef);
        }
    }

    public static <T> T unzip(byte[] zippedBytes, ObjectMapper objectMapper, Class<T> valueType) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            unzipToStream(zippedBytes, bos);
            return objectMapper.readValue(bos.toByteArray(), valueType);
        }
    }
}
