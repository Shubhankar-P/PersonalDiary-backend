package net.shubhankarpotnis.diaryApp.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectIdSerializerCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(ObjectId.class, new JsonSerializer<ObjectId>() {
                @Override
                public void serialize(ObjectId value, JsonGenerator gen,
                                      SerializerProvider serializers) throws IOException {
                    gen.writeString(value.toHexString());
                }
            });
            builder.modules(module, new JavaTimeModule());
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
            );
        };
    }
}