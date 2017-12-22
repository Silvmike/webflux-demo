package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

public class LoggingServerHttpRequestDecorator extends ServerHttpRequestDecorator implements WithMemoizingFunction {

    private final Logger logger;
    private final MediaTypeFilter mediaTypeFilter;

    public LoggingServerHttpRequestDecorator(ServerHttpRequest delegate, Logger logger, MediaTypeFilter mediaTypeFilter) {
        super(delegate);
        this.logger = logger;
        this.mediaTypeFilter = mediaTypeFilter;
        flushLog(EMPTY_BYTE_ARRAY_OUTPUT_STREAM); // getBody() isn't called when controller doesn't need it.
    }

    @Override
    public Flux<DataBuffer> getBody() {
        MediaType mediaType = getHeaders().getContentType();
        if (logger.isDebugEnabled() && mediaTypeFilter.logged(mediaType)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            return super.getBody().map(getMemoizingFunction(baos)).doOnComplete(() -> flushLog(baos));
        } else {
            return super.getBody().doOnComplete(() -> flushLog(EMPTY_BYTE_ARRAY_OUTPUT_STREAM));
        }
    }

    private void flushLog(ByteArrayOutputStream baos) {
        MediaType mediaType = getHeaders().getContentType();
        boolean logged = mediaTypeFilter.logged(mediaType);
        if (logger.isInfoEnabled()) {
            StringBuffer data = new StringBuffer();
            data.append('[').append(getMethodValue())
                    .append("] '").append(String.valueOf(getURI()))
                    .append("' from ")
                    .append(
                            Optional.ofNullable(getRemoteAddress())
                                    .map(addr -> addr.getHostString())
                                    .orElse("null")
                    );
            if (logger.isDebugEnabled() && baos != EMPTY_BYTE_ARRAY_OUTPUT_STREAM) {
                if (logged) {
                    data.append(" with payload [\n");
                    if (mediaTypeFilter.encoded(mediaType)) {
                        data.append(new HexBinaryAdapter().marshal(baos.toByteArray()));
                    } else {
                        data.append(baos.toString());
                    }
                    data.append("\n]");
                }
                logger.debug(data.toString());
            } else {
                logger.info(data.toString());
            }

        }
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

}
