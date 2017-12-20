package ru.hardcoders.demo.webflux.web_handler.annotated.flux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FooController {

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<Foo> bar() {
        return Mono.just(new Foo("OK"));
    }

}
