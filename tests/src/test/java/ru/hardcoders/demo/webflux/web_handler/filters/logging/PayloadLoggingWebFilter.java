package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.Optional;

public class PayloadLoggingWebFilter implements WebFilter {

    private static final ByteArrayOutputStream EMPTY_BYTE_ARRAY_OUTPUT_STREAM = new ByteArrayOutputStream(0);

    private final Logger logger;
    private final boolean encodeBytes;

    public PayloadLoggingWebFilter(Logger logger) {
        this(logger, false);
    }

    public PayloadLoggingWebFilter(Logger logger, boolean encodeBytes) {
        this.logger = logger;
        this.encodeBytes = encodeBytes;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (logger.isInfoEnabled()) {
            return chain.filter(decorate(exchange));
        } else {
            return chain.filter(exchange);
        }
    }

    private ServerWebExchange decorate(ServerWebExchange exchange) {
        return new ServerWebExchangeDecorator(exchange) {

            @Override
            public ServerHttpRequest getRequest() {
                return new ServerHttpRequestDecorator(super.getRequest()) {

                    @Override
                    public Flux<DataBuffer> getBody() {

                        if (logger.isDebugEnabled()) {
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            return super.getBody().map(dataBuffer -> {
                                try {
                                    Channels.newChannel(baos).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                                } catch (IOException e) {
                                    logger.error("Unable to log input request due to an error", e);
                                }
                                return dataBuffer;
                            }).doOnComplete(() -> flushLog(baos));

                        } else {
                            return super.getBody().doOnComplete(() -> flushLog(EMPTY_BYTE_ARRAY_OUTPUT_STREAM));
                        }
                    }
                };
            }

            private void flushLog(ByteArrayOutputStream baos) {
                ServerHttpRequest request = super.getRequest();
                if (logger.isInfoEnabled()) {
                    StringBuffer data = new StringBuffer();
                    data.append('[').append(request.getMethodValue())
                        .append("] '").append(String.valueOf(request.getURI()))
                        .append("' from ")
                            .append(
                                Optional.ofNullable(request.getRemoteAddress())
                                            .map(addr -> addr.getHostString())
                                        .orElse("null")
                            );
                    if (logger.isDebugEnabled()) {
                        data.append(" with payload [\n");
                        if (encodeBytes) {
                            data.append(new HexBinaryAdapter().marshal(baos.toByteArray()));
                        } else {
                            data.append(baos.toString());
                        }
                        data.append("\n]");
                        logger.debug(data.toString());
                    } else {
                        logger.info(data.toString());
                    }

                }
            }
        };
    }

}
