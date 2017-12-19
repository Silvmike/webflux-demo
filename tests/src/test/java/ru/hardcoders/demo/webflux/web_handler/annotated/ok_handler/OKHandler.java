package ru.hardcoders.demo.webflux.web_handler.annotated.ok_handler;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "/")
public class OKHandler {

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String handle() {
        return "\"OK\"";
    }

}
