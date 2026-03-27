package com.weatherdashboard.api.client;

import com.weatherdashboard.api.client.dto.OpenWeatherMapResponse;
import com.weatherdashboard.api.config.WeatherApiProperties;
import com.weatherdashboard.api.dto.WeatherResponse;
import com.weatherdashboard.api.exception.ExternalApiException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

/**
 * Implementation of WeatherApiClient using OpenWeatherMap API.
 * Handles HTTP calls, exception mapping, and internal DTO conversion.
 * Note: Bean registration is handled by WeatherApiConfig, not @Component.
 */
public class WeatherApiClientImpl implements WeatherApiClient {
    private final RestTemplate restTemplate;
    private final WeatherApiProperties properties;

    public WeatherApiClientImpl(RestTemplate restTemplate, WeatherApiProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Calls the external weather API and converts response to internal DTO.
     *
     * @param cityName the name of the city
     * @return WeatherResponse internal DTO
     * @throws ExternalApiException on API errors
     */
    @Override
    public WeatherResponse getWeather(String cityName) throws ExternalApiException {
        try {
            String url = String.format("%s/weather?q=%s&appid=%s",
                    properties.getBaseUrl(),
                    cityName,
                    properties.getKey());

            OpenWeatherMapResponse response = restTemplate.getForObject(url, OpenWeatherMapResponse.class);

            if (response == null) {
                throw new ExternalApiException("Empty response from weather API", 500, null);
            }

            return mapToInternal(response);

        } catch (HttpClientErrorException e) {
            throw new ExternalApiException("Client error: " + e.getMessage(), e.getStatusCode().value(), e);
        } catch (HttpServerErrorException e) {
            throw new ExternalApiException("Server error: " + e.getMessage(), e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            // Check if it's a timeout
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new ExternalApiException("API request timeout", 0, e);
            }
            throw new ExternalApiException("Network error: " + e.getMessage(), 0, e);
        } catch (Exception e) {
            throw new ExternalApiException("Unexpected error: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Maps OpenWeatherMap response to internal WeatherResponse DTO.
     *
     * @param external the external API response
     * @return internal WeatherResponse
     */
    private WeatherResponse mapToInternal(OpenWeatherMapResponse external) {
        String cityName = external.getName();
        BigDecimal temperature = external.getMain().getTemp() != null
                ? BigDecimal.valueOf(external.getMain().getTemp())
                : BigDecimal.ZERO;
        String condition = mapCondition(external.getWeather()[0].getMain());
        Integer humidity = external.getMain().getHumidity();
        BigDecimal windSpeed = external.getWind().getSpeed() != null
                ? BigDecimal.valueOf(external.getWind().getSpeed())
                : BigDecimal.ZERO;

        return new WeatherResponse(cityName, temperature, condition, humidity, windSpeed);
    }

    /**
     * Maps external weather condition to normalized internal condition string.
     *
     * @param externalCondition the external API condition string
     * @return normalized condition string
     */
    private String mapCondition(String externalCondition) {
        if (externalCondition == null) {
            return "UNKNOWN";
        }

        return switch (externalCondition.toUpperCase()) {
            case "CLEAR" -> "CLEAR";
            case "CLOUDS" -> "CLOUDY";
            case "RAIN", "DRIZZLE" -> "RAINY";
            case "THUNDERSTORM" -> "STORMY";
            case "SNOW" -> "SNOWY";
            case "MIST", "SMOKE", "HAZE", "DUST", "FOG", "SAND", "ASH", "SQUALL", "TORNADO" -> "MISTY";
            default -> "UNKNOWN";
        };
    }

}
