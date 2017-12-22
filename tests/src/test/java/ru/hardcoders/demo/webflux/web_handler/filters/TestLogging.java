package ru.hardcoders.demo.webflux.web_handler.filters;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.testng.annotations.Test;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;
import ru.hardcoders.demo.webflux.web_handler.filters.logging.RequestLoggingWebFilter;
import ru.hardcoders.demo.webflux.web_handler.filters.logging.ResponseLoggingWebFilter;

import java.util.Arrays;
import java.util.Collections;

public class TestLogging {

    private static final String TEST_STRING = "TEST";
    private static final String EXPECTED_LOG_RESULT_REQUEST = "[POST] 'http://127.0.0.1:9999/?name=xyz' from null";
    private static final String EXPECTED_LOG_RESULT_REQUEST_DEBUG = "[POST] 'http://127.0.0.1:9999/?name=xyz' from 127.0.0.1\n" +
            "user-agent=[ReactorNetty/0.7.2.RELEASE]\n" +
            "transfer-encoding=[chunked]\n" +
            "host=[127.0.0.1:9999]\n" +
            "accept=[*/*]\n" +
            "accept-encoding=[gzip]\n" +
            "WebTestClient-Request-Id=[1]\n" +
            "Content-Type=[text/plain;charset=UTF-8]\n[\nTEST\n]";

    private static final String EXPECTED_LOG_RESULT_RESPONSE = "Response for [POST] 'http://127.0.0.1:9999/?name=xyz' from null";
    private static final String EXPECTED_LOG_RESULT_RESPONSE_DEBUG = "Response for [POST] 'http://127.0.0.1:9999/?name=xyz' from 127.0.0.1\n" +
            "Content-Type=[application/json;charset=UTF-8]\n" +
            "[\n4\n]";

    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void testDebug() throws Exception {

        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(Boolean.TRUE);
        Mockito.when(logger.isInfoEnabled()).thenReturn(Boolean.TRUE);

        DispatcherHandler dispatcherHandler = buildWebHandler();

        ReactorHttpHandlerAdapter httpHandlerAdapter = new ReactorHttpHandlerAdapter(
                new HttpWebHandlerAdapter(
                        new FilteringWebHandler(
                                dispatcherHandler,
                                Arrays.asList(new RequestLoggingWebFilter(logger), new ResponseLoggingWebFilter(logger))
                        )
                )
        );

        NettyContext nettyContext = HttpServer.create("127.0.0.1", 9999).newHandler(httpHandlerAdapter).block();
        final WebTestClient testClient = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:9999/").build();

        testClient.post().uri("/?name=xyz")
                .body(BodyInserters.fromObject(TEST_STRING))
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(String.valueOf(TEST_STRING.length()));

        Mockito.verify(logger, Mockito.atLeastOnce()).debug(EXPECTED_LOG_RESULT_REQUEST_DEBUG);
        Mockito.verify(logger, Mockito.times(1)).debug(EXPECTED_LOG_RESULT_RESPONSE_DEBUG);

        nettyContext.dispose();

    }

    @Test
    public void testInfo() throws Exception {

        Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(Boolean.FALSE);
        Mockito.when(logger.isInfoEnabled()).thenReturn(Boolean.TRUE);

        DispatcherHandler dispatcherHandler = buildWebHandler();
        final WebTestClient testClient = WebTestClient.bindToWebHandler(new FilteringWebHandler(
                dispatcherHandler,
                Arrays.asList(new RequestLoggingWebFilter(logger), new ResponseLoggingWebFilter(logger))
        )).configureClient().baseUrl("http://127.0.0.1:9999/").build();

        testClient.post().uri("/?name=xyz")
                .body(BodyInserters.fromObject(TEST_STRING))
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(String.valueOf(TEST_STRING.length()));

        Mockito.verify(logger, Mockito.atLeastOnce()).info(EXPECTED_LOG_RESULT_REQUEST);
        Mockito.verify(logger, Mockito.times(1)).info(EXPECTED_LOG_RESULT_RESPONSE);

    }

    private DispatcherHandler buildWebHandler() throws Exception {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // handler mapping
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        beanFactory.registerSingleton("handlerMapping", handlerMapping);

        // handler adapter
        RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
        handlerAdapter.setMessageReaders(
            Collections.singletonList(
                new DecoderHttpMessageReader<>(
                        StringDecoder.allMimeTypes(false)
                )
            )
        );
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
        beanFactory.registerSingleton("testHandler", new TestLoggingController());

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(beanFactory);

        applicationContext.refresh();

        handlerAdapter.setApplicationContext(applicationContext);
        handlerAdapter.afterPropertiesSet();

        handlerMapping.setApplicationContext(applicationContext);
        handlerMapping.afterPropertiesSet();

        return new DispatcherHandler(applicationContext);
    }

}
