package com.weatherdashboard.api.service;

import com.weatherdashboard.api.client.WeatherApiClient;
import com.weatherdashboard.api.dto.WeatherResponse;
import com.weatherdashboard.api.exception.ExternalApiException;
import com.weatherdashboard.api.exception.WeatherServiceException;
import org.springframework.stereotype.Service;

/**
 * Service layer for weather operations.
 * Contains business logic and coordinates between controller and external API
 * client.
 */
@Service
public class WeatherService {
    private final WeatherApiClient weatherApiClient;

    public WeatherService(WeatherApiClient weatherApiClient) {
        this.weatherApiClient = weatherApiClient;
    }

    /**
     * Retrieves weather information for a given city.
     * Validates input and delegates to API client.
     *
     * @param cityName the name of the city
     * @return WeatherResponse with weather data
     * @throws WeatherServiceException on validation or API errors
     */
    public WeatherResponse getWeatherByCity(String cityName) throws WeatherServiceException {
        try {
            return weatherApiClient.getWeather(cityName);
        } catch (ExternalApiException e) {
            throw new WeatherServiceException("Error retrieving weather: " + e.getMessage(), "API_ERROR", e);
        }
    }

}