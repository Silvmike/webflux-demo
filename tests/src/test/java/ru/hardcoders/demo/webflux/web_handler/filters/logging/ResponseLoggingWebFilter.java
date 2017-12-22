package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class ResponseLoggingWebFilter implements WebFilter {

    private static final MediaTypeFilter DEFAULT_FILTER = new MediaTypeFilter() {};

    private final Logger logger;

    private MediaTypeFilter mediaTypeFilter;

    private LogMessageFormatter responseMessageFromatter;

    public ResponseLoggingWebFilter(Logger logger) {
        this(logger, DEFAULT_FILTER);
    }

    public ResponseLoggingWebFilter(Logger logger, MediaTypeFilter mediaTypeFilter) {
        this.logger = logger;
        this.mediaTypeFilter = mediaTypeFilter;
        this.responseMessageFromatter = new LoggingServerHttpResponseDecorator.DefaultLogMessageFormatter();
    }

    public MediaTypeFilter getMediaTypeFilter() {
        return mediaTypeFilter;
    }

    public void setMediaTypeFilter(MediaTypeFilter mediaTypeFilter) {
        this.mediaTypeFilter = mediaTypeFilter;
    }

    public LogMessageFormatter getResponseMessageFromatter() {
        return responseMessageFromatter;
    }

    public void setResponseMessageFromatter(LogMessageFormatter responseMessageFromatter) {
        this.responseMessageFromatter = responseMessageFromatter;
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

        final ServerHttpResponse decoratedResponse = new LoggingServerHttpResponseDecorator(
                exchange.getResponse(),
                exchange.getRequest(),
                logger,
                mediaTypeFilter,
                new LoggingServerHttpResponseDecorator.DefaultLogMessageFormatter()
        );

        return new ServerWebExchangeDecorator(exchange) {

            @Override
            public ServerHttpResponse getResponse() {
                return decoratedResponse;
            }

        };
    }

}