package ru.hardcoders.demo.webflux.web_handler.netty.async_controller;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.resources.PoolResources;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class FooController implements DisposableBean {

    private final ExecutorService executor = Executors.newFixedThreadPool(TestSyncVsAsync.TEST_COUNT);

    @GetMapping(value = "/sync/{timeout}")
    public Mono<Foo> sync(@PathVariable Integer timeout) {
        try {
            Thread.currentThread().sleep(Duration.ofSeconds(timeout.longValue()).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return Mono.just(new Foo("OK"));
    }

    @GetMapping(value = "/async/{timeout}")
    public Mono<Foo> async(@PathVariable Integer timeout) throws InterruptedException {
        return Mono.fromFuture(CompletableFuture.supplyAsync(() -> {
            try {
                Thread.currentThread().sleep(Duration.ofSeconds(timeout.longValue()).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            return new Foo("OK");
        }, executor));
    }

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }

}
