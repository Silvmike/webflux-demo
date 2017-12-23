package ru.hardcoders.demo.webflux.web_handler.filters.size;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

public class MaxPayloadSizeFilter implements WebFilter {

    private final long maxPayloadSize;

    public MaxPayloadSizeFilter(long maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(new ServerWebExchangeDecorator(exchange) {

            final ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {

                final Flux<DataBuffer> decoratedBody = decorate(super.getBody());

                @Override
                public Flux<DataBuffer> getBody() {
                    return decoratedBody;
                }

                private Flux<DataBuffer> decorate(Flux<DataBuffer> body) {
                    final AtomicLong size = createCounter();
                    return body.flatMap(dataBuffer -> {
                        if (size.addAndGet(dataBuffer.readableByteCount()) > maxPayloadSize) {
                            return Flux.error(new TooLongPayloadException("too long payload: read " + size.get() + " bytes, max is " + maxPayloadSize));
                        }
                        return Flux.just(dataBuffer);
                    });

                }

            };

            @Override
            public ServerHttpRequest getRequest() {
                return decoratedRequest;
            }
        });
    }

    AtomicLong createCounter() {
        return new AtomicLong();
    }

    public static class TooLongPayloadException extends RuntimeException {

        public TooLongPayloadException(String message) {
            super(message);
        }

    }

}
