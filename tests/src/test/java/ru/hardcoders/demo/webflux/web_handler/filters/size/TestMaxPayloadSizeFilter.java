package ru.hardcoders.demo.webflux.web_handler.filters.size;

import java.util.concurrent.atomic.AtomicLong;

public class TestMaxPayloadSizeFilter extends MaxPayloadSizeFilter {

    public volatile AtomicLong lastCounter;

    public TestMaxPayloadSizeFilter(long maxPayloadSize) {
        super(maxPayloadSize);
    }

    @Override
    AtomicLong createCounter() {
        lastCounter = super.createCounter();
        return lastCounter;
    }

}
