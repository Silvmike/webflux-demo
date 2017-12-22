package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class PayloadLoggingWebFilter implements WebFilter {

    public static final MediaTypeFilter DEFAULT_FILTER = new MediaTypeFilter() {};
    public static final MediaTypeFilter ENCODING_ALL_FILTER = new MediaTypeFilter() {
        @Override
        public boolean encoded(MediaType mediaType) {
            return true;
        }
    };

    private Logger logger;

    private MediaTypeFilter mediaTypeFilter;

    public PayloadLoggingWebFilter(Logger logger) {
        this(logger, DEFAULT_FILTER);
    }

    public PayloadLoggingWebFilter(Logger logger, MediaTypeFilter mediaTypeFilter) {
        this.logger = logger;
        this.mediaTypeFilter = mediaTypeFilter;
    }

    public MediaTypeFilter getMediaTypeFilter() {
        return mediaTypeFilter;
    }

    public void setMediaTypeFilter(MediaTypeFilter mediaTypeFilter) {
        this.mediaTypeFilter = mediaTypeFilter;
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
        final ServerHttpRequest decoratedRequest = new LoggingServerHttpRequestDecorator(exchange.getRequest(), logger, mediaTypeFilter);
        final ServerHttpResponse decoratedResponse = new LoggingServerHttpResponseDecorator(exchange.getResponse(), exchange.getRequest(), logger, mediaTypeFilter);
        return new ServerWebExchangeDecorator(exchange) {

            @Override
            public ServerHttpRequest getRequest() {
                return decoratedRequest;
            }

            @Override
            public ServerHttpResponse getResponse() {
                return decoratedResponse;
            }

        };
    }

}