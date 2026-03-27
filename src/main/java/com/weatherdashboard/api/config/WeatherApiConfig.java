package com.weatherdashboard.api.config;

import com.weatherdashboard.api.client.WeatherApiClient;
import com.weatherdashboard.api.client.WeatherApiClientImpl;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Spring configuration for Weather API beans.
 * Configures RestTemplate with timeouts and registers WeatherApiClient.
 */
@Configuration
public class WeatherApiConfig {

    /**
     * Creates a RestTemplate bean with timeout configuration.
     *
     * @param builder    RestTemplateBuilder injected by Spring
     * @param properties WeatherApiProperties for timeout value
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, WeatherApiProperties properties) {
        return builder.connectTimeout(Duration.ofMillis(properties.getTimeoutMs())).readTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
    }

    /**
     * Creates WeatherApiClient bean.
     *
     * @param restTemplate the configured RestTemplate
     * @param properties   the API properties
     * @return WeatherApiClient implementation
     */
    @Bean
    public WeatherApiClient weatherApiClient(RestTemplate restTemplate, WeatherApiProperties properties) {
        return new WeatherApiClientImpl(restTemplate, properties);
    }

}
