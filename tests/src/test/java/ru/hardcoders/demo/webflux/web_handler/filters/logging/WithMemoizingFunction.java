package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.function.Function;

interface WithMemoizingFunction {

    ByteArrayOutputStream EMPTY_BYTE_ARRAY_OUTPUT_STREAM = new ByteArrayOutputStream(0) {

        @Override
        public synchronized void write(int b) {
            throw new UnsupportedOperationException("stub");
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            throw new UnsupportedOperationException("stub");
        }

        @Override
        public synchronized void writeTo(OutputStream out) throws IOException {
            throw new UnsupportedOperationException("stub");
        }

    };

    Logger getLogger();

    default Function<DataBuffer, DataBuffer> memoizingFunction(ByteArrayOutputStream baos) {
        return dataBuffer -> {
            try {
                Channels.newChannel(baos).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
            } catch (IOException e) {
                getLogger().error("Unable to log input request due to an error", e);
            }
            return dataBuffer;
        };
    }

}
