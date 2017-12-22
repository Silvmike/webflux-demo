package ru.hardcoders.demo.webflux.web_handler.filters.logging;

public interface PayloadAdapter {

    default String toString(byte[] payload) {
        return new String(payload);
    }

}
