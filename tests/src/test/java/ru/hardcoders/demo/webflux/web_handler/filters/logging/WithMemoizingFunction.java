package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.function.Function;

interface WithMemoizingFunction {

    ByteArrayOutputStream EMPTY_BYTE_ARRAY_OUTPUT_STREAM = new ByteArrayOutputStream(0);

    Logger getLogger();

    default Function<DataBuffer, DataBuffer> getMemoizingFunction(ByteArrayOutputStream baos) {
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
