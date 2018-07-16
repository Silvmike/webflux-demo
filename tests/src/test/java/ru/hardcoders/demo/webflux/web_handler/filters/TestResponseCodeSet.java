package ru.hardcoders.demo.webflux.web_handler.filters;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.testng.annotations.Test;
import reactor.core.publisher.Mono;

import java.util.Collections;

public class TestResponseCodeSet {

    @Test
    public void testSetStatusCode() {

        final WebTestClient testClient = WebTestClient.bindToWebHandler(new FilteringWebHandler(
                x -> Mono.empty(),
                Collections.singletonList(this::filter)
        )).configureClient().baseUrl("http://127.0.0.1:9999/").build();

        testClient.post().uri("/?name=xyz")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.I_AM_A_TEAPOT);

    }

    private Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().setStatusCode(HttpStatus.I_AM_A_TEAPOT);
        return Mono.empty();
    }

}
