package ru.hardcoders.demo.webflux.web_handler.filters;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.hardcoders.demo.webflux.web_handler.filters.size.MaxPayloadSizeFilter;

@RestController
public class MessageLengthController {

    @PostMapping(value = "/", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Integer anyData(@RequestBody String body) {
        return body.length();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MaxPayloadSizeFilter.TooLongPayloadException.class)
    public void handleMaxPayloadSizeError() {}

}
