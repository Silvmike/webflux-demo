package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

class LoggingServerHttpRequestDecorator extends ServerHttpRequestDecorator implements WithMemoizingFunction {

    private final Logger logger;
    private final MediaTypeFilter mediaTypeFilter;
    private final PayloadAdapter payloadAdapter;
    private final Flux<DataBuffer> decoratedBody;

    public LoggingServerHttpRequestDecorator(ServerHttpRequest delegate, Logger logger, MediaTypeFilter mediaTypeFilter, PayloadAdapter payloadAdapter) {
        super(delegate);
        this.logger = logger;
        this.mediaTypeFilter = mediaTypeFilter;
        this.payloadAdapter = payloadAdapter;
        this.decoratedBody = decorateBody(delegate.getBody());
        flushLog(EMPTY_BYTE_ARRAY_OUTPUT_STREAM); // getBody() isn't called when controller doesn't need it.
    }

    private Flux<DataBuffer> decorateBody(Flux<DataBuffer> body) {
        MediaType mediaType = getHeaders().getContentType();
        if (logger.isDebugEnabled() && mediaTypeFilter.logged(mediaType)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            return body.map(getMemoizingFunction(baos)).doOnComplete(() -> flushLog(baos));
        } else {
            return body.doOnComplete(() -> flushLog(EMPTY_BYTE_ARRAY_OUTPUT_STREAM));
        }
    }

    @Override
    public Flux<DataBuffer> getBody() {
        return this.decoratedBody;
    }

    private void flushLog(ByteArrayOutputStream baos) {
        if (logger.isInfoEnabled()) {
            StringBuilder data = new StringBuilder();
            data.append('[').append(getMethodValue())
                    .append("] '").append(String.valueOf(getURI()))
                    .append("' from ")
                    .append(
                            Optional.ofNullable(getRemoteAddress())
                                    .map(addr -> addr.getHostString())
                                    .orElse("null")
                    );
            if (logger.isDebugEnabled()) {
                if (mediaTypeFilter.logged(getHeaders().getContentType())) {
                    data.append(" with payload [\n");
                    data.append(payloadAdapter.toString(baos.toByteArray()));
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
