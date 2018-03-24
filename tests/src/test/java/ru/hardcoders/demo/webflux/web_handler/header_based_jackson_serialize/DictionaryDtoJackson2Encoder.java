package ru.hardcoders.demo.webflux.web_handler.header_based_jackson_serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.AbstractJackson2Encoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryDtoJackson2Encoder extends AbstractJackson2Encoder {

    public static final String LOCALE_KEY = "locale";

    private final LocaleContextResolver localeContextResolver;

    public DictionaryDtoJackson2Encoder(ObjectMapper mapper, LocaleContextResolver localeContextResolver) {
        super(mapper, new MimeType[]{MediaType.APPLICATION_JSON_UTF8 });
        this.localeContextResolver = localeContextResolver;
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return getMimeTypes();
    }

    @Override
    public Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType, MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {
        Map<String, Object> hints = new HashMap<>(super.getEncodeHints(actualType, elementType, mediaType, request, response));
        ServerWebExchange exchange = new LocaleResolverWebExchange(request, response, this.localeContextResolver);
        hints.put(LOCALE_KEY, exchange.getLocaleContext().getLocale().getLanguage());
        return hints;
    }

    @Override
    protected ObjectWriter customizeWriter(ObjectWriter writer, MimeType mimeType, ResolvableType elementType, Map<String, Object> hints) {
        final ObjectWriter newWriter = super.customizeWriter(writer, mimeType, elementType, hints);
        SerializationConfig serializationConfig = newWriter.getConfig().with(newWriter.getAttributes().withPerCallAttribute(LOCALE_KEY, hints.get(LOCALE_KEY)));
        return new ObjectWriter(newWriter, serializationConfig) {};
    }

}
