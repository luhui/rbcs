package com.example.rbcs.web.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer initJackson() {
        return builder -> {
            //自定义Long类型转换 超过15个数字用String格式返回，由于js的number只能表示15个数字
            builder.serializerByType(Long.class, new MToStringSerializer());
            builder.serializerByType(Long.TYPE, new MToStringSerializer());
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);//不包含为空的字段
            builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);//不包含空字符串字段
            builder.indentOutput(true).dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            builder.serializerByType(Date.class, new DateToLongSerializer());
        };
    }

    public static class MToStringSerializer extends JsonSerializer<Long> {

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null && value.toString().length() > 15) {
                gen.writeString(value.toString());
            } else if (value != null) {
                gen.writeNumber(value);
            }
        }
    }

    public static class DateToLongSerializer extends JsonSerializer<Date> {

        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeNumber(value.getTime());
            }
        }
    }
}
