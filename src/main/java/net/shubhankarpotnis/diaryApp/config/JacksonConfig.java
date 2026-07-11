package net.shubhankarpotnis.diaryApp.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;

@Configuration
public class JacksonConfig {

    // The ObjectId serializer that used to live here is gone -- IDs are now
    // plain Long values, which Jackson serializes natively with no custom
    // module needed. We keep this bean only for the date-formatting setting
    // below, so LocalDateTime fields (like DiaryEntry.date) keep serializing
    // as readable ISO-8601 strings ("2026-07-10T14:30:00") instead of Jackson's
    // default raw epoch-millis timestamps.
    @Bean
    public JsonMapperBuilderCustomizer dateFormatCustomizer() {
        return builder -> builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}