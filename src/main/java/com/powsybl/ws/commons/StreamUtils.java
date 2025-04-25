package com.powsybl.ws.commons;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

public final class StreamUtils {
    private StreamUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static Consumer<OutputStream> getStreamer(InputStream inputStream, int packageSize) {
        return outputStream -> {
            try (inputStream) {
                byte[] buffer = new byte[packageSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
