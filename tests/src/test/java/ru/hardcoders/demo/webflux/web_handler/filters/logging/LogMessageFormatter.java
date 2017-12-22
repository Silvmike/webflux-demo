package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public interface LogMessageFormatter {

    String format(ServerHttpRequest request, ServerHttpResponse response, byte[] payload);

}
