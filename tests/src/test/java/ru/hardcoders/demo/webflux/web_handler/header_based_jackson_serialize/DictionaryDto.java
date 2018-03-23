package ru.hardcoders.demo.webflux.web_handler.header_based_jackson_serialize;

import java.util.HashMap;
import java.util.Map;

public class DictionaryDto {

    private Map<String, String> value = new HashMap<>();

    public void add(String locale, String message) {
        value.put(locale, message);
    }

    public String getLocaled(String locale) {
        return value.get(locale);
    }

}
