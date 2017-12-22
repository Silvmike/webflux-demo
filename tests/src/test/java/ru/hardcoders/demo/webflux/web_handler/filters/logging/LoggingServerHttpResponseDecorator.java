package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

class LoggingServerHttpResponseDecorator extends ServerHttpResponseDecorator implements WithMemoizingFunction {

    private final Logger logger;
    private final MediaTypeFilter mediaTypeFilter;
    private final ByteArrayOutputStream baos;
    private final ServerHttpRequest request;
    private final LogMessageFormatter formatter;

    LoggingServerHttpResponseDecorator(ServerHttpResponse delegate, ServerHttpRequest request, Logger logger, MediaTypeFilter mediaTypeFilter, LogMessageFormatter formatter) {
        super(delegate);
        this.logger = logger;
        this.mediaTypeFilter = mediaTypeFilter;
        this.request = request;
        this.formatter = formatter;
        MediaType mediaType = getHeaders().getContentType();
        if (logger.isDebugEnabled() && mediaTypeFilter.logged(mediaType)) {
            baos = new ByteArrayOutputStream();
            delegate.beforeCommit(() -> {
                flushLog(baos);
                return Mono.empty();
            });
        } else if (logger.isInfoEnabled()) {
            baos = EMPTY_BYTE_ARRAY_OUTPUT_STREAM;
            delegate.beforeCommit(() -> {
                flushLog(baos);
                return Mono.empty();
            });
        } else {
            baos = EMPTY_BYTE_ARRAY_OUTPUT_STREAM;
        }
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        if (baos != EMPTY_BYTE_ARRAY_OUTPUT_STREAM) {
            return super.writeWith(Flux.from(body).map(getMemoizingFunction(baos)));
        } else {
            return super.writeWith(body);
        }
    }

    @Override
    public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        if (baos != EMPTY_BYTE_ARRAY_OUTPUT_STREAM) {
            return super.writeAndFlushWith(Flux.from(body).map( x -> {
                return Flux.from(x).map(getMemoizingFunction(baos));
            } ));
        } else {
            return super.writeAndFlushWith(body);
        }
    }

    private void flushLog(ByteArrayOutputStream baos) {
        if (logger.isInfoEnabled()) {
            if (logger.isDebugEnabled()) {
                if (mediaTypeFilter.logged(getHeaders().getContentType())) {
                    logger.debug(formatter.format(this.request, getDelegate(), baos.toByteArray()));
                }
                logger.debug(formatter.format(this.request, getDelegate(), null));
            } else {
                logger.info(formatter.format(this.request, getDelegate(), null));
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
            final StringBuilder data = new StringBuilder();
            data.append("Response for [").append(request.getMethodValue())
                    .append("] '").append(String.valueOf(request.getURI()))
                    .append("' from ")
                    .append(
                            Optional.ofNullable(request.getRemoteAddress())
                                    .map( addr -> addr.getHostString() )
                                    .orElse("null")
                    );
            response.getHeaders().forEach((key, value) -> data.append('\n').append(key).append('=').append(String.valueOf(value)));
            if (payload != null) {
                data.append("\n[\n");
                data.append(new String(payload));
                data.append("\n]");
            }
            return data.toString();
        }
    }

}