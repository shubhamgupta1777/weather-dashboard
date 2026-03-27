package com.weatherdashboard.api.client;

import com.weatherdashboard.api.dto.WeatherResponse;
import com.weatherdashboard.api.exception.ExternalApiException;

/**
 * Interface for external weather API communication.
 * Abstracts the underlying API provider.
 */
public interface WeatherApiClient {

    /**
     * Retrieves weather information for a given city.
     *
     * @param cityName the name of the city
     * @return WeatherResponse with weather data
     * @throws ExternalApiException if API call fails
     */
    WeatherResponse getWeather(String cityName) throws ExternalApiException;

}
