package ru.hardcoders.demo.webflux.web_handler.header_based_jackson_serialize;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class LocaledController {

    public static final String RU_TEXT = "Привет, Мир!";
    public static final String EN_TEXT = "Hello, World!";

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<DictionaryDto> get() {
        DictionaryDto dictionaryDto = new DictionaryDto();
        dictionaryDto.add("ru", RU_TEXT);
        dictionaryDto.add("en", EN_TEXT);
        return Mono.just(dictionaryDto);
    }

}
