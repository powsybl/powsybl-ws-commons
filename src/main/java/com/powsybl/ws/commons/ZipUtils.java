package com.powsybl.ws.commons;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
