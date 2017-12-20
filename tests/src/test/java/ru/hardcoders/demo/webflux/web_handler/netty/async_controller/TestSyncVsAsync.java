package ru.hardcoders.demo.webflux.web_handler.netty.async_controller;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.testng.Assert;
import org.testng.annotations.Test;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.HttpResources;
import reactor.ipc.netty.http.server.HttpServer;
import reactor.ipc.netty.resources.LoopResources;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestSyncVsAsync {

    public static final int TEST_COUNT = LoopResources.DEFAULT_IO_WORKER_COUNT * 2;
    public static final int awaitTimeSeconds = 2;
    public static final long EXPECTED_TIME = ((TEST_COUNT + 1) * Duration.ofSeconds(awaitTimeSeconds).toMillis()) / LoopResources.DEFAULT_IO_WORKER_COUNT;

    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void testSyncNetty() throws Exception {

        DispatcherHandler dispatcherHandler = buildWebHandler();

        ReactorHttpHandlerAdapter httpHandlerAdapter = new ReactorHttpHandlerAdapter(
                new HttpWebHandlerAdapter(dispatcherHandler)
        );

        NettyContext nettyContext = HttpServer.create("127.0.0.1", 9999).newHandler(httpHandlerAdapter).block();
        final WebTestClient testClient = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:9999/")
                                                .responseTimeout(Duration.ofSeconds(awaitTimeSeconds * (TEST_COUNT + 1)))
                                                .build();

        StopWatch watch = new StopWatch();

        applyTest(testClient, watch, "sync");

        Assert.assertTrue(watch.getTotalTimeMillis() > EXPECTED_TIME);

        nettyContext.dispose();

    }

    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void testAsyncNetty() throws Exception {

        DispatcherHandler dispatcherHandler = buildWebHandler();

        ReactorHttpHandlerAdapter httpHandlerAdapter = new ReactorHttpHandlerAdapter(
                new HttpWebHandlerAdapter(dispatcherHandler)
        );

        NettyContext nettyContext = HttpServer.create("127.0.0.1", 9999).newHandler(httpHandlerAdapter).block();
        final WebTestClient testClient = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:9999/")
                                                .responseTimeout(Duration.ofSeconds(awaitTimeSeconds * (TEST_COUNT + 1)))
                                                .build();

        StopWatch watch = new StopWatch();

        applyTest(testClient, watch, "async");

        Assert.assertTrue(watch.getTotalTimeMillis() < EXPECTED_TIME);

        nettyContext.dispose();

    }

    @Test(enabled = false) /* to run manually, starts server on localhost */
    public void testSyncNettyWithCustomizedThreadPool() throws Exception {

        DispatcherHandler dispatcherHandler = buildWebHandler();

        ReactorHttpHandlerAdapter httpHandlerAdapter = new ReactorHttpHandlerAdapter(
                new HttpWebHandlerAdapter(dispatcherHandler)
        );

        HttpResources.set(LoopResources.create("mypool", TEST_COUNT, false));

        NettyContext nettyContext = HttpServer.create("127.0.0.1", 9999).newHandler(httpHandlerAdapter).block();
        final WebTestClient testClient = WebTestClient.bindToServer().baseUrl("http://127.0.0.1:9999/")
                .responseTimeout(Duration.ofSeconds(awaitTimeSeconds * (TEST_COUNT + 1)))
                .build();

        StopWatch watch = new StopWatch();

        applyTest(testClient, watch, "sync");

        Assert.assertTrue(watch.getTotalTimeMillis() < EXPECTED_TIME);

        nettyContext.dispose();

    }

    private void applyTest(WebTestClient testClient, StopWatch watch, final String service) throws InterruptedException, java.util.concurrent.ExecutionException {
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future> futures = new LinkedList<>();
        watch.start();
        for (int i = 0; i<TEST_COUNT; i++) {
            futures.add(executor.submit(() -> {
                testClient.get().uri("/" + service + "/" + awaitTimeSeconds).exchange().expectStatus().isOk();
            }));
        }
        for (Future future : futures) {
            future.get();
        }
        executor.shutdown();
        watch.stop();
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

    private void assertTestResult(WebTestClient testClient) {
        testClient.get().uri("/")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.bar").isEqualTo("OK");
    }

}
