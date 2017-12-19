package ru.hardcoders.demo.webflux.http_handler;

import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.testng.annotations.Test;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

public class TestSimpleFunctionalEndpointsServer {

    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void testWithNetty() {

        ReactorHttpHandlerAdapter reactorHttpHandlerAdapter = new ReactorHttpHandlerAdapter(
                RouterFunctions.toHttpHandler(testRouterFunction())
        );

        NettyContext nettyContext = HttpServer.create("127.0.0.1", 9999)
                                              .newHandler(reactorHttpHandlerAdapter)
                                              .block();

        WebTestClient testClient = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:9999/").build();

        assertTestResult(testClient);

        nettyContext.dispose();

    }

    private RouterFunction<ServerResponse> testRouterFunction() {
        return RouterFunctions.route(RequestPredicates.GET("/"), (request) -> {
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(BodyInserters.fromObject("\"OK\""));
        });
    }

    @Test
    public void testWithoutServer() {
        WebTestClient testClient = WebTestClient.bindToRouterFunction(testRouterFunction()).build();
        assertTestResult(testClient);
    }

    private void assertTestResult(WebTestClient testClient) {
        testClient.get().uri("/")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("\"OK\"");
    }

}
