package net.shubhankarpotnis.diaryApp.config;

import org.bson.types.ObjectId;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer objectIdSerializerCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(ObjectId.class, new ValueSerializer<ObjectId>() {
                @Override
                public void serialize(ObjectId value, JsonGenerator gen, SerializationContext context) {
                    gen.writeString(value.toHexString());
                }
            });
            builder.addModule(module);
            builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}