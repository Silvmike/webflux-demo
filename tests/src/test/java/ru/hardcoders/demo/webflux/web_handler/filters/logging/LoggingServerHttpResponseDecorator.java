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

public class LoggingServerHttpResponseDecorator extends ServerHttpResponseDecorator implements WithMemoizingFunction {

    private final Logger logger;
    private final MediaTypeFilter mediaTypeFilter;
    private final ByteArrayOutputStream baos;
    private final ServerHttpRequest request;
    private final PayloadAdapter payloadAdapter;

    public LoggingServerHttpResponseDecorator(ServerHttpResponse delegate, ServerHttpRequest request, Logger logger, MediaTypeFilter mediaTypeFilter, PayloadAdapter payloadAdapter) {
        super(delegate);
        this.logger = logger;
        this.mediaTypeFilter = mediaTypeFilter;
        this.request = request;
        this.payloadAdapter = payloadAdapter;
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
        MediaType mediaType = getHeaders().getContentType();
        boolean logged = mediaTypeFilter.logged(mediaType);
        if (logger.isInfoEnabled()) {
            final StringBuffer data = new StringBuffer();
            data.append("Response for [").append(request.getMethodValue())
                    .append("] '").append(String.valueOf(request.getURI()))
                    .append("' from ")
                    .append(
                            Optional.ofNullable(request.getRemoteAddress())
                                    .map( addr -> addr.getHostString() )
                                    .orElse("null")
                    );
            getHeaders().entrySet().forEach( entry ->  {
                data.append('\n').append(entry.getKey()).append('=').append(String.valueOf(entry.getValue()));
            } );
            if (logger.isDebugEnabled()) {
                if (logged) {
                    data.append("\n[\n");
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