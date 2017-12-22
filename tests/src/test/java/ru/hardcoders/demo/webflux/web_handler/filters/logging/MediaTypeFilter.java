package ru.hardcoders.demo.webflux.web_handler.filters.logging;

import org.springframework.http.MediaType;

public interface MediaTypeFilter {

    default boolean logged(MediaType mediaType) {
        return true;
    }

}
