package com.campus.activity.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reject fractional identifiers and counts instead of silently truncating them. */
@Configuration
public class JsonInputConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictJsonInputs() {
        return builder -> builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
}
