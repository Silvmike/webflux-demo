package ru.hardcoders.demo.webflux.web_handler.header_based_jackson_serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

class DictionaryDtoModule extends SimpleModule {

    public DictionaryDtoModule() {
        addSerializer(DictionaryDto.class, new StdSerializer<DictionaryDto>(DictionaryDto.class) {
            @Override
            public void serialize(DictionaryDto value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                String locale = (String) provider.getAttribute(DictionaryDtoJackson2Encoder.LOCALE_KEY);
                gen.writeRawValue(value.getLocaled(locale));
            }
        });
    }

}
