package com.epam.edp.demo.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson configuration to enforce strict type handling.
 * Rejects non-string JSON values (numbers, booleans, arrays) for String fields.
 * This prevents numeric passwords like {"password": 123456} from being silently coerced.
 * Also enables FAIL_ON_UNKNOWN_PROPERTIES globally so that unknown fields in request
 * bodies (e.g. sending "password" to PUT /users/{id}/name) are rejected with 400.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictStringCustomizer() {
        return builder -> {
            SimpleModule strictStringModule = new SimpleModule("StrictStringModule");
            strictStringModule.addDeserializer(String.class, new StrictStringDeserializer());
            // Use modulesToInstall so auto-detected modules (JavaTimeModule etc.) are NOT replaced
            builder.modulesToInstall(strictStringModule);
            builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }

    /**
     * Custom deserializer that only accepts JSON string tokens for String fields.
     * Rejects numbers, booleans, and other non-string JSON types.
     */
    static class StrictStringDeserializer extends StringDeserializer {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            switch (p.currentToken()) {
                case VALUE_STRING:
                    return p.getText();
                case VALUE_NULL:
                    return null;
                default:
                    ctxt.reportInputMismatch(String.class,
                            "Expected a string value but got: %s", p.currentToken());
                    return null; // unreachable
            }
        }
    }
}

