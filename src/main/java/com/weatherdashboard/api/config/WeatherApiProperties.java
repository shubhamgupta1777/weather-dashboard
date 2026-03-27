package com.weatherdashboard.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for external weather API.
 * Binds properties from application.yaml with prefix "weather.api"
 */
@Component
@ConfigurationProperties(prefix = "weather.api")
public class WeatherApiProperties {
    private String key;
    private String baseUrl;
    private long timeoutMs = 5000;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

}
