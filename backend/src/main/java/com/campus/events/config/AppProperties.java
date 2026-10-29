package com.campus.events.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String storageRoot,
        String publicBaseUrl,
        String checkinSecret,
        String adminUsername,
        String adminPassword,
        String adminDisplayName,
        boolean seedDemo) {
}
