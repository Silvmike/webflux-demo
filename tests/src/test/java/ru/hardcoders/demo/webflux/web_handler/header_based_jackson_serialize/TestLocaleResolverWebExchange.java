package ru.hardcoders.demo.webflux.web_handler.header_based_jackson_serialize;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.FixedLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Locale;

public class TestLocaleResolverWebExchange {

    @Test
    public void test() {

        for (String locale : new String[] {"en", "ru"}) {

            ServerWebExchange webExchange = buildExchange(new AcceptHeaderLocaleContextResolver(), locale);
            LocaleContext localeContext = webExchange.getLocaleContext();

            Assert.assertEquals(localeContext.getLocale().getLanguage(), locale);

        }

    }

    @Test
    public void testFixed() {

        Locale expectedLocale = Locale.getDefault();

        for (String locale : new String[] {"en", "ru"}) {

            ServerWebExchange webExchange = buildExchange(new FixedLocaleContextResolver(expectedLocale), locale);
            LocaleContext localeContext = webExchange.getLocaleContext();

            Assert.assertEquals(localeContext.getLocale(), expectedLocale);

        }

    }

    private ServerWebExchange buildExchange(LocaleContextResolver resolver, String acceptLanguage) {
        return new LocaleResolverWebExchange(
                MockServerHttpRequest.method(
                        HttpMethod.GET,
                        URI.create("http://127.0.0.1/")
                )
                        .header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage)
                        .build(),
                new MockServerHttpResponse(),
                resolver
        );
    }

}
