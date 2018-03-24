package ru.hardcoders.demo.webflux.web_handler.header_based_jackson_serialize;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.i18n.LocaleContextResolver;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

public class LocaleResolverWebExchange implements ServerWebExchange {

    private final ServerHttpRequest request;
    private final ServerHttpResponse response;
    private final LocaleContextResolver localeContextResolver;

    public LocaleResolverWebExchange(ServerHttpRequest request, ServerHttpResponse response, LocaleContextResolver localeContextResolver) {
        this.request = request;
        this.response = response;
        this.localeContextResolver = localeContextResolver;
    }

    @Override
    public ServerHttpRequest getRequest() {
        return this.request;
    }

    @Override
    public ServerHttpResponse getResponse() {
        return this.response;
    }

    @Override
    public Map<String, Object> getAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<WebSession> getSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Principal> Mono<T> getPrincipal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<MultiValueMap<String, String>> getFormData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<MultiValueMap<String, Part>> getMultipartData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocaleContext getLocaleContext() {
        return this.localeContextResolver.resolveLocaleContext(this);
    }

    @Override
    public boolean isNotModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkNotModified(Instant lastModified) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkNotModified(String etag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkNotModified(String etag, Instant lastModified) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String transformUrl(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUrlTransformer(Function<String, String> transformer) {
        throw new UnsupportedOperationException();
    }

}
