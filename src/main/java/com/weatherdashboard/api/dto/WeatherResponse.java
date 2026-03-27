package com.weatherdashboard.api.dto;

import java.math.BigDecimal;

/**
 * Response DTO for weather information.
 * Immutable record used throughout all application layers.
 * Never exposes external API response structure.
 */
public record WeatherResponse(
        String cityName,
        BigDecimal temperature,
        String condition,
        Integer humidity,
        BigDecimal windSpeed) {
}
