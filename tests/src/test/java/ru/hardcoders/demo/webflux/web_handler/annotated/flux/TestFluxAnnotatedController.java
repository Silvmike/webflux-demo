package ru.hardcoders.demo.webflux.web_handler.annotated.flux;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.testng.annotations.Test;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

import java.util.Collections;

public class TestFluxAnnotatedController {


    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void testWithNetty() throws Exception {

        DispatcherHandler dispatcherHandler = buildWebHandler();

        ReactorHttpHandlerAdapter httpHandlerAdapter = new ReactorHttpHandlerAdapter(
                new HttpWebHandlerAdapter(dispatcherHandler)
        );

        NettyContext nettyContext = HttpServer.create("127.0.0.1", 9999).newHandler(httpHandlerAdapter).block();
        WebTestClient testClient = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:9999/").build();

        assertTestResult(testClient);

        nettyContext.dispose();

    }

    @Test
    public void testWithoutServer() throws Exception {
        WebTestClient testClient = WebTestClient.bindToWebHandler(buildWebHandler()).build();
        assertTestResult(testClient);
    }

    private DispatcherHandler buildWebHandler() throws Exception {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // handler mapping
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        beanFactory.registerSingleton("handlerMapping", handlerMapping);

        // handler adapter
        RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
        beanFactory.registerSingleton("handlerAdapter", handlerAdapter);

        // HandlerResultHandler
        ResponseBodyResultHandler responseResultHandler = new ResponseBodyResultHandler(
                Collections.singletonList(
                        new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder())
                ),
                new FixedContentTypeResolver(MediaType.APPLICATION_JSON_UTF8)
        );

        beanFactory.registerSingleton("responseHandler", responseResultHandler);

        // controller
        beanFactory.registerSingleton("fooHandler", new FooController());

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(beanFactory);

        applicationContext.refresh();

        handlerAdapter.setApplicationContext(applicationContext);
        handlerAdapter.afterPropertiesSet();

        handlerMapping.setApplicationContext(applicationContext);
        handlerMapping.afterPropertiesSet();

        return new DispatcherHandler(applicationContext);
    }


    @Test
    public void testWithController() {

        WebTestClient testClient = WebTestClient.bindToController(new FooController())
                                                .httpMessageCodecs(cfg -> cfg.defaultCodecs())
                                                .build();
        assertTestResult(testClient);

    }

    private void assertTestResult(WebTestClient testClient) {
        testClient.get().uri("/")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.bar").isEqualTo("OK");
    }

}
