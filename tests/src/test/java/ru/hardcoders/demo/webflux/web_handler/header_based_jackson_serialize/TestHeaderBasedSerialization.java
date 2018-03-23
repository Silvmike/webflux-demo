package ru.hardcoders.demo.webflux.web_handler.header_based_jackson_serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.testng.Assert;
import org.testng.annotations.Test;
import reactor.ipc.netty.http.server.HttpServer;

import java.util.Collections;

public class TestHeaderBasedSerialization {

    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void test() throws Exception {

        DispatcherHandler dispatcherHandler = buildWebHandler();

        HttpServer.create("127.0.0.1", 9999).newHandler(
                new ReactorHttpHandlerAdapter(
                        new HttpWebHandlerAdapter(
                                dispatcherHandler
                        )
                )).block();

        final WebTestClient testClient = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:9999/").build();

        testInLocale(testClient, "en", LocaledController.EN_TEXT);
        testInLocale(testClient, "ru", LocaledController.RU_TEXT);

    }

    @Test
    public void testWithoutServer() throws Exception {

        DispatcherHandler dispatcherHandler = buildWebHandler();

        final WebTestClient testClient = WebTestClient.bindToWebHandler(dispatcherHandler)
                                                      .configureClient()
                                                      .baseUrl("http://127.0.0.1:9999/")
                                                      .build();

        testInLocale(testClient, "en", LocaledController.EN_TEXT);
        testInLocale(testClient, "ru", LocaledController.RU_TEXT);

    }

    private void testInLocale(WebTestClient testClient, String locale, String expectedResult) {
        byte[] enResult = testClient.get().uri("/")
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
                .exchange()
                .expectStatus().isOk().expectBody().returnResult().getResponseBody();

        Assert.assertEquals(enResult, ("\"" + expectedResult + "\"").getBytes());
    }

    private DispatcherHandler buildWebHandler() throws Exception {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // handler mapping
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        beanFactory.registerSingleton("handlerMapping", handlerMapping);

        // handler adapter
        RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
        beanFactory.registerSingleton("handlerAdapter", handlerAdapter);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new DictionaryDtoModule());

        // HandlerResultHandler
        ResponseBodyResultHandler responseResultHandler = new ResponseBodyResultHandler(
                Collections.singletonList(
                        new EncoderHttpMessageWriter<>(new DictionaryDtoJackson2Encoder(mapper))
                ),
                new FixedContentTypeResolver(MediaType.APPLICATION_JSON_UTF8)
        );

        beanFactory.registerSingleton("responseHandler", responseResultHandler);

        // controller
        beanFactory.registerSingleton("testHandler", new LocaledController());

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(beanFactory);

        applicationContext.refresh();

        handlerAdapter.setApplicationContext(applicationContext);
        handlerAdapter.afterPropertiesSet();

        handlerMapping.setApplicationContext(applicationContext);
        handlerMapping.afterPropertiesSet();

        return new DispatcherHandler(applicationContext);
    }

}
