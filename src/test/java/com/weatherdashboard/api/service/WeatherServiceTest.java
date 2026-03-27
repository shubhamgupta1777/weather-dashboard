package com.weatherdashboard.api.service;

import com.weatherdashboard.api.client.WeatherApiClient;
import com.weatherdashboard.api.dto.WeatherResponse;
import com.weatherdashboard.api.exception.ExternalApiException;
import com.weatherdashboard.api.exception.WeatherServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeatherService.
 * Tests the service layer business logic and coordination with API client.
 */
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherApiClient weatherApiClient;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(weatherApiClient);
    }

    /**
     * Test: Valid city name returns WeatherResponse with all fields populated
     * Arranges: Mock client returns valid WeatherResponse
     * Acts: Call getWeatherByCity with valid city
     * Asserts: Response matches expected values, mock called once
     */
    @Test
    void testGetWeatherByCity_validCity_returnsWeatherResponse() throws ExternalApiException, WeatherServiceException {
        // Arrange
        String cityName = "London";
        WeatherResponse expectedResponse = new WeatherResponse(
                cityName,
                new BigDecimal("15.5"),
                "CLOUDY",
                72,
                new BigDecimal("12.3"));

        when(weatherApiClient.getWeather(cityName))
                .thenReturn(expectedResponse);

        // Act
        WeatherResponse actualResponse = weatherService.getWeatherByCity(cityName);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.cityName(), actualResponse.cityName());
        assertEquals(expectedResponse.temperature(), actualResponse.temperature());
        assertEquals(expectedResponse.condition(), actualResponse.condition());
        assertEquals(expectedResponse.humidity(), actualResponse.humidity());
        assertEquals(expectedResponse.windSpeed(), actualResponse.windSpeed());

        verify(weatherApiClient, times(1)).getWeather(cityName);
    }

    /**
     * Test: API client exception is wrapped as WeatherServiceException
     * Arranges: Mock client throws ExternalApiException
     * Acts: Call getWeatherByCity
     * Asserts: WeatherServiceException is thrown with correct message and error
     * code
     */
    @Test
    void testGetWeatherByCity_clientThrowsException_wrapsAsWeatherServiceException()
            throws ExternalApiException {
        // Arrange
        String cityName = "Paris";
        ExternalApiException clientException = new ExternalApiException("API error", 500, null);

        when(weatherApiClient.getWeather(cityName))
                .thenThrow(clientException);

        // Act & Assert
        WeatherServiceException exception = assertThrows(
                WeatherServiceException.class,
                () -> weatherService.getWeatherByCity(cityName));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Error retrieving weather"));
        assertEquals("API_ERROR", exception.getErrorCode());

        verify(weatherApiClient, times(1)).getWeather(cityName);
    }

    /**
     * Test: Multiple calls for same city fetch fresh data each time
     * Arranges: Mock client returns different temperatures on successive calls
     * Acts: Call service twice for same city
     * Asserts: Both responses have different values, mock called twice
     */
    @Test
    void testGetWeatherByCity_repeatedQueries_fetchesFreshData()
            throws ExternalApiException, WeatherServiceException {
        // Arrange
        String cityName = "Berlin";
        WeatherResponse firstResponse = new WeatherResponse(
                cityName,
                new BigDecimal("10.0"),
                "RAINY",
                80,
                new BigDecimal("15.0"));
        WeatherResponse secondResponse = new WeatherResponse(
                cityName,
                new BigDecimal("12.0"),
                "CLOUDY",
                75,
                new BigDecimal("10.0"));

        when(weatherApiClient.getWeather(cityName))
                .thenReturn(firstResponse)
                .thenReturn(secondResponse);

        // Act
        WeatherResponse firstCall = weatherService.getWeatherByCity(cityName);
        WeatherResponse secondCall = weatherService.getWeatherByCity(cityName);

        // Assert
        assertNotEquals(firstCall.temperature(), secondCall.temperature());
        assertNotEquals(firstCall.condition(), secondCall.condition());
        assertEquals(firstCall.temperature(), new BigDecimal("10.0"));
        assertEquals(secondCall.temperature(), new BigDecimal("12.0"));

        verify(weatherApiClient, times(2)).getWeather(cityName);
    }

    /**
     * Test: DTO mapping converts all fields correctly
     * Arranges: Mock client returns populated WeatherResponse
     * Acts: Call service
     * Asserts: All fields are present and correctly mapped
     */
    @Test
    void testGetWeatherByCity_dtoMapping_convertsExternalToInternal()
            throws ExternalApiException, WeatherServiceException {
        // Arrange
        String cityName = "Tokyo";
        WeatherResponse mappedResponse = new WeatherResponse(
                cityName,
                new BigDecimal("25.0"),
                "CLEAR",
                60,
                new BigDecimal("8.5"));

        when(weatherApiClient.getWeather(cityName))
                .thenReturn(mappedResponse);

        // Act
        WeatherResponse result = weatherService.getWeatherByCity(cityName);

        // Assert
        assertAll("All fields mapped correctly",
                () -> assertEquals("Tokyo", result.cityName()),
                () -> assertEquals(new BigDecimal("25.0"), result.temperature()),
                () -> assertEquals("CLEAR", result.condition()),
                () -> assertEquals(60, result.humidity()),
                () -> assertEquals(new BigDecimal("8.5"), result.windSpeed()));
    }
}
