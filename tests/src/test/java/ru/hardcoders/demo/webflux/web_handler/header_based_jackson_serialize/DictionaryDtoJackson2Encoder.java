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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictionaryDtoJackson2Encoder extends AbstractJackson2Encoder {

    public static final String LOCALE_KEY = "locale";

    public DictionaryDtoJackson2Encoder(ObjectMapper mapper) {
        super(mapper, new MimeType[]{MediaType.APPLICATION_JSON_UTF8 });
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return getMimeTypes();
    }

    @Override
    public Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType, MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {
        Map<String, Object> hints = new HashMap<>(super.getEncodeHints(actualType, elementType, mediaType, request, response));
        hints.put(LOCALE_KEY, request.getHeaders().getAcceptLanguage().iterator().next().getRange());
        return hints;
    }

    @Override
    protected ObjectWriter customizeWriter(ObjectWriter writer, MimeType mimeType, ResolvableType elementType, Map<String, Object> hints) {
        ObjectWriter newWriter = super.customizeWriter(writer, mimeType, elementType, hints);
        SerializationConfig serializationConfig = newWriter.getConfig().with(writer.getAttributes().withPerCallAttribute("locale", hints.get("locale")));
        return new ObjectWriter(newWriter, serializationConfig) {};
    }

}
