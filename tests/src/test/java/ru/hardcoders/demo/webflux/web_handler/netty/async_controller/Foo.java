package ru.hardcoders.demo.webflux.web_handler.netty.async_controller;

class Foo {

    private String bar;

    public Foo() {}

    public Foo(String bar) {
        this.bar = bar;
    }

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }

    @Override
    public String toString() {
        return "Foo{" +
                "bar='" + bar + '\'' +
                '}';
    }
}
