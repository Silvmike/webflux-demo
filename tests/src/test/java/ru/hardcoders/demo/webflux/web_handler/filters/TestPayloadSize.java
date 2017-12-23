package ru.hardcoders.demo.webflux.web_handler.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
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
import org.testng.Assert;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;
import reactor.ipc.netty.http.server.HttpServer;
import ru.hardcoders.demo.webflux.web_handler.filters.size.TestMaxPayloadSizeFilter;

import java.util.Collections;

public class TestPayloadSize {

    private static final Logger logger = LoggerFactory.getLogger(TestPayloadSize.class);

    private static final int TEST_PAYLOAD_COUNT = 10;
    private static final int TEST_PAYLOAD_SIZE = 8192;
    private static final int TEST_FULL_PAYLOAD_SIZE = TEST_PAYLOAD_SIZE * TEST_PAYLOAD_COUNT;

    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void test() throws Exception {

        TestMaxPayloadSizeFilter testMaxPayloadSizeFilter = new TestMaxPayloadSizeFilter(10);

        DispatcherHandler dispatcherHandler = buildWebHandler();

        HttpServer.create("127.0.0.1", 9999).newHandler(
                new ReactorHttpHandlerAdapter(
                        new HttpWebHandlerAdapter(
                            new FilteringWebHandler(
                                    dispatcherHandler,
                                    Collections.singletonList(testMaxPayloadSizeFilter)
                            )
                        )
                )).block();

        final WebTestClient testClient = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:9999/").build();

        Flux<DataBuffer> generatedBody = generateBody();

        testClient.post().uri("/?name=xyz")
                .contentType(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromDataBuffers(generatedBody))
                .exchange()
                .expectStatus().isBadRequest();

        Assert.assertTrue(testMaxPayloadSizeFilter.lastCounter.get() < TEST_FULL_PAYLOAD_SIZE);
        logger.info("Read bytes: " + testMaxPayloadSizeFilter.lastCounter.get());

    }

    private Flux<DataBuffer> generateBody() {
        DefaultDataBufferFactory defaultDataBufferFactory = new DefaultDataBufferFactory();
        Flux<DataBuffer> flux = Flux.just();
        for (int i = 0; i<TEST_PAYLOAD_COUNT; i++) {
            byte[] payload = new byte[TEST_FULL_PAYLOAD_SIZE];
            for (int j = 0; j<TEST_FULL_PAYLOAD_SIZE; j++) {
                payload[i] = (byte) (i % 256);
            }
            flux = flux.concatWith(Flux.just(defaultDataBufferFactory.wrap(payload)));
        }
        return flux;
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
        beanFactory.registerSingleton("testHandler", new MessageLengthController());

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(beanFactory);

        applicationContext.refresh();

        handlerAdapter.setApplicationContext(applicationContext);
        handlerAdapter.afterPropertiesSet();

        handlerMapping.setApplicationContext(applicationContext);
        handlerMapping.afterPropertiesSet();

        return new DispatcherHandler(applicationContext);
    }

}
