package com.weatherdashboard.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weatherdashboard.api.client.WeatherApiClient;
import com.weatherdashboard.api.dto.WeatherResponse;
import com.weatherdashboard.api.exception.ExternalApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WeatherController.
 * Tests the full request/response cycle and HTTP status code mapping.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeatherApiClient weatherApiClient;

    /**
     * Test: Valid city name returns HTTP 200 with all weather fields
     * Arranges: Mock API client with valid response
     * Acts: Perform GET /weather/city/London
     * Asserts: HTTP 200, response has all 5 fields with correct values
     */
    @Test
    void testGetWeatherByCity_validCity_returns200() throws Exception {
        // Arrange
        String cityName = "London";
        WeatherResponse weatherResponse = new WeatherResponse(
                cityName,
                new BigDecimal("15.5"),
                "CLOUDY",
                72,
                new BigDecimal("12.3"));

        when(weatherApiClient.getWeather(cityName))
                .thenReturn(weatherResponse);

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/weather/city/{cityName}", cityName))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.cityName", equalTo("London")))
                .andExpect(jsonPath("$.temperature", equalTo(15.5)))
                .andExpect(jsonPath("$.condition", equalTo("CLOUDY")))
                .andExpect(jsonPath("$.humidity", equalTo(72)))
                .andExpect(jsonPath("$.windSpeed", equalTo(12.3)))
                .andReturn();

        // Verify response body structure
        String responseBody = result.getResponse().getContentAsString();
        WeatherResponse responseDto = objectMapper.readValue(responseBody, WeatherResponse.class);

        assert responseDto.cityName().equals("London");
        assert responseDto.temperature().equals(new BigDecimal("15.5"));
        assert responseDto.condition().equals("CLOUDY");
        assert responseDto.humidity() == 72;
        assert responseDto.windSpeed().equals(new BigDecimal("12.3"));
    }

    /**
     * Test: Second valid request returns fresh data (independent requests)
     * Arranges: Mock API client with different response on second call
     * Acts: Perform two GET requests for same city
     * Asserts: Both return HTTP 200 with different data
     */
    @Test
    void testGetWeatherByCity_repeatedRequests_returnsFreshData() throws Exception {
        // Arrange
        String cityName = "Paris";
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

        // Act & Assert - First request
        mockMvc.perform(get("/weather/city/{cityName}", cityName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperature", equalTo(10.0)))
                .andExpect(jsonPath("$.condition", equalTo("RAINY")));

        // Act & Assert - Second request
        mockMvc.perform(get("/weather/city/{cityName}", cityName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperature", equalTo(12.0)))
                .andExpect(jsonPath("$.condition", equalTo("CLOUDY")));
    }

    /**
     * Test: Multiple different cities all return correct data
     * Arranges: Mock API client with different responses for different cities
     * Acts: Perform GET requests for different cities
     * Asserts: Each returns HTTP 200 with correct city data
     */
    @Test
    void testGetWeatherByCity_multipleCities_eachReturnsCorrectData() throws Exception {
        // Arrange
        WeatherResponse londonResponse = new WeatherResponse(
                "London",
                new BigDecimal("15.5"),
                "CLOUDY",
                72,
                new BigDecimal("12.3"));
        WeatherResponse tokyoResponse = new WeatherResponse(
                "Tokyo",
                new BigDecimal("25.0"),
                "CLEAR",
                60,
                new BigDecimal("8.5"));

        when(weatherApiClient.getWeather("London"))
                .thenReturn(londonResponse);
        when(weatherApiClient.getWeather("Tokyo"))
                .thenReturn(tokyoResponse);

        // Act & Assert - London
        mockMvc.perform(get("/weather/city/London"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName", equalTo("London")))
                .andExpect(jsonPath("$.temperature", equalTo(15.5)))
                .andExpect(jsonPath("$.condition", equalTo("CLOUDY")));

        // Act & Assert - Tokyo
        mockMvc.perform(get("/weather/city/Tokyo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName", equalTo("Tokyo")))
                .andExpect(jsonPath("$.temperature", equalTo(25.0)))
                .andExpect(jsonPath("$.condition", equalTo("CLEAR")));
    }

    /**
     * Test: Response contains only internal DTO fields, not raw API response
     * Arranges: Mock API client with response
     * Acts: Perform GET request
     * Asserts: Response JSON has exactly 5 expected fields, no extra fields
     */
    @Test
    void testGetWeatherByCity_response_isInternalDtoNotRawApi() throws Exception {
        // Arrange
        String cityName = "Berlin";
        WeatherResponse weatherResponse = new WeatherResponse(
                cityName,
                new BigDecimal("20.0"),
                "SUNNY",
                65,
                new BigDecimal("9.0"));

        when(weatherApiClient.getWeather(cityName))
                .thenReturn(weatherResponse);

        // Act & Assert
        mockMvc.perform(get("/weather/city/{cityName}", cityName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("cityName")))
                .andExpect(jsonPath("$", hasKey("temperature")))
                .andExpect(jsonPath("$", hasKey("condition")))
                .andExpect(jsonPath("$", hasKey("humidity")))
                .andExpect(jsonPath("$", hasKey("windSpeed")))
                // Verify no external API fields present
                .andExpect(jsonPath("$", not(hasKey("main"))))
                .andExpect(jsonPath("$", not(hasKey("weather"))))
                .andExpect(jsonPath("$", not(hasKey("wind"))))
                .andExpect(jsonPath("$", not(hasKey("name"))));
    }
}
