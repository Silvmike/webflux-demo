package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.slf4j.Logger;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class RequestLoggingWebFilter implements WebFilter {

    private static final MediaTypeFilter DEFAULT_FILTER = new MediaTypeFilter() {};

    private final Logger logger;

    private MediaTypeFilter mediaTypeFilter;

    private LogMessageFormatter logMessageFormatter;

    public RequestLoggingWebFilter(Logger logger) {
        this(logger, DEFAULT_FILTER);
    }

    public RequestLoggingWebFilter(Logger logger, MediaTypeFilter mediaTypeFilter) {
        this.logger = logger;
        this.mediaTypeFilter = mediaTypeFilter;
        this.logMessageFormatter = new LoggingServerHttpRequestDecorator.DefaultLogMessageFormatter();
    }

    public MediaTypeFilter getMediaTypeFilter() {
        return mediaTypeFilter;
    }

    public void setMediaTypeFilter(MediaTypeFilter mediaTypeFilter) {
        this.mediaTypeFilter = mediaTypeFilter;
    }

    public LogMessageFormatter getLogMessageFormatter() {
        return logMessageFormatter;
    }

    public void setLogMessageFormatter(LogMessageFormatter logMessageFormatter) {
        this.logMessageFormatter = logMessageFormatter;
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

        final ServerHttpRequest decoratedRequest = new LoggingServerHttpRequestDecorator(
                exchange.getRequest(),
                exchange.getResponse(),
                logger,
                mediaTypeFilter,
                new LoggingServerHttpRequestDecorator.DefaultLogMessageFormatter()
        );

        return new ServerWebExchangeDecorator(exchange) {

            @Override
            public ServerHttpRequest getRequest() {
                return decoratedRequest;
            }

        };
    }

}