package com.gitssie.openapi.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gitssie.openapi.service.Provider;
import io.ebean.Database;
import io.ebean.bean.EntityBean;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class JacksonConfiguration {

    public static class StdModule extends SimpleModule {
        private DateTimeFormatter dateTimeFormatter;

        public StdModule(JacksonProperties properties) {
            this.dateTimeFormatter = DateTimeFormatter.ofPattern(properties.getDateFormat(), ObjectUtils.defaultIfNull(properties.getLocale(), Locale.CHINA));

            addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
            addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        }

        public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                if (provider.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
                    long timestamp = value.atZone(provider.getTimeZone().toZoneId()).toInstant().toEpochMilli();
                    gen.writeNumber(timestamp);
                } else {
                    gen.writeString(value.format(dateTimeFormatter));
                }
            }
        }

        private class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext provider) throws IOException {
                String dateTimeString = p.getValueAsString();
                if (provider.isEnabled(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)) {
                    long timestamp = p.getValueAsLong();
                    Instant instant = Instant.ofEpochMilli(timestamp);
                    return LocalDateTime.ofInstant(instant, provider.getTimeZone().toZoneId());
                } else {
                    return LocalDateTime.parse(dateTimeString, dateTimeFormatter);
                }
            }
        }
    }

    public static class EntityBeanModule extends SimpleModule {
        private ObjectProvider<Provider> provider;

        public EntityBeanModule(JacksonProperties properties, ObjectProvider<Provider> provider) {
            this.provider = provider;
            addSerializer(EntityBean.class, new EntityBeanSerializer());
            addDeserializer(EntityBean.class, new EntityBeanDeserializer());
        }

        public class EntityBeanSerializer extends JsonSerializer<EntityBean> {
            @Override
            public void serialize(EntityBean value, JsonGenerator gen, SerializerProvider p) throws IOException {
                provider.getIfAvailable().json().toJson(value, gen);
            }
        }

        private class EntityBeanDeserializer extends JsonDeserializer<EntityBean> {
            @Override
            public EntityBean deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                Class<? extends EntityBean> targetType = (Class<? extends EntityBean>) ctx.getContextualType().getRawClass();
                return provider.getIfAvailable().json().toBean(targetType, p);
            }
        }
    }
}
