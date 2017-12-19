package ru.hardcoders.demo.webflux.web_handler;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.testng.annotations.Test;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

import java.util.Collections;

public class TestSimpleWebHandlerServer {


    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void testWithNetty() {

        DispatcherHandler dispatcherHandler = buildWebHandler();

        ReactorHttpHandlerAdapter reactorHttpHandlerAdapter = new ReactorHttpHandlerAdapter(
                new HttpWebHandlerAdapter(dispatcherHandler)
        );

        NettyContext nettyContext = HttpServer.create("127.0.0.1", 9999)
                                              .newHandler(reactorHttpHandlerAdapter)
                                              .block();

        WebTestClient testClient = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:9999/").build();

        assertTestResult(testClient);

        nettyContext.dispose();

    }

    @Test
    public void testWithoutServer() {
        WebTestClient testClient = WebTestClient.bindToWebHandler(buildWebHandler()).build();
        assertTestResult(testClient);
    }

    /**
     * See <a href=https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-special-bean-types>documentation</a>
     */
    private DispatcherHandler buildWebHandler() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        // HandlerMapping
        beanFactory.registerBeanDefinition("routerFunctionMapping",
                BeanDefinitionBuilder.genericBeanDefinition(RouterFunctionMapping.class)
                        .addConstructorArgValue(testRouterFunction())
                        .getBeanDefinition()
        );
        // HandlerAdapter
        beanFactory.registerSingleton("handlerAdapter", new HandlerFunctionAdapter());

        // HandlerResultHandler
        ServerResponseResultHandler responseResultHandler = new ServerResponseResultHandler();
        responseResultHandler.setMessageWriters(
                Collections.singletonList(
                        new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes())
                )
        );
        beanFactory.registerSingleton("responseHandler", responseResultHandler);

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(beanFactory);
        applicationContext.refresh();

        return new DispatcherHandler(applicationContext);
    }

    private RouterFunction<ServerResponse> testRouterFunction() {
        return RouterFunctions.route(RequestPredicates.GET("/"), (request) -> {
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .body(BodyInserters.fromObject("\"OK\""));
        });
    }

    private void assertTestResult(WebTestClient testClient) {
        testClient.get().uri("/")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("\"OK\"");
    }

}
