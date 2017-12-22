package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

class LoggingServerHttpRequestDecorator extends ServerHttpRequestDecorator implements WithMemoizingFunction {

    private final Logger logger;
    private final MediaTypeFilter mediaTypeFilter;
    private final LogMessageFormatter formatter;
    private final Flux<DataBuffer> decoratedBody;
    private final ServerHttpResponse response;

    LoggingServerHttpRequestDecorator(ServerHttpRequest delegate, ServerHttpResponse response, Logger logger, MediaTypeFilter mediaTypeFilter, LogMessageFormatter formatter) {
        super(delegate);
        this.logger = logger;
        this.mediaTypeFilter = mediaTypeFilter;
        this.formatter = formatter;
        this.decoratedBody = decorateBody(delegate.getBody());
        this.response = response;
        flushLog(EMPTY_BYTE_ARRAY_OUTPUT_STREAM, true); // getBody() isn't called when controller doesn't need it.
    }

    private Flux<DataBuffer> decorateBody(Flux<DataBuffer> body) {
        MediaType mediaType = getHeaders().getContentType();
        if (logger.isDebugEnabled() && mediaTypeFilter.logged(mediaType)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            return body.map(memoizingFunction(baos)).doOnComplete(() -> flushLog(baos, false));
        } else {
            return body.doOnComplete(() -> flushLog(EMPTY_BYTE_ARRAY_OUTPUT_STREAM, false));
        }
    }

    @Override
    public Flux<DataBuffer> getBody() {
        return this.decoratedBody;
    }

    private void flushLog(ByteArrayOutputStream baos, boolean onCreate) {
        if (logger.isInfoEnabled()) {
            if (logger.isDebugEnabled()) {
                if (mediaTypeFilter.logged(getHeaders().getContentType())) {
                    logger.debug(formatter.format(getDelegate(), this.response, onCreate ? null : baos.toByteArray()));
                } else {
                    logger.debug(formatter.format(getDelegate(), this.response, null));
                }
            } else {
                logger.info(formatter.format(getDelegate(), this.response, null));
            }
        }
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    static final class DefaultLogMessageFormatter implements LogMessageFormatter {

        @Override
        public String format(ServerHttpRequest request, ServerHttpResponse response, byte[] payload) {
            StringBuilder data = new StringBuilder();
            data.append('[').append(request.getMethodValue())
                    .append("] '").append(String.valueOf(request.getURI()))
                    .append("' from ")
                    .append(
                            Optional.ofNullable(request.getRemoteAddress())
                                    .map(addr -> addr.getHostString())
                                    .orElse("null")
                    );
            if (payload != null) {
                request.getHeaders().forEach((key, value) -> data.append('\n').append(key).append('=').append(String.valueOf(value)));
                data.append("\n[\n");
                data.append(new String(payload));
                data.append("\n]");
            }
            return data.toString();
        }
    }

}
