package com.weatherdashboard.api.controller;

import com.weatherdashboard.api.dto.ErrorResponse;
import com.weatherdashboard.api.dto.WeatherResponse;
import com.weatherdashboard.api.exception.WeatherServiceException;
import com.weatherdashboard.api.service.WeatherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for weather endpoints.
 * Handles HTTP requests and delegates to service layer.
 * Contains no business logic.
 */
@RestController
@RequestMapping("/weather")
public class WeatherController {
    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * GET endpoint to retrieve weather by city name.
     * 
     * @param cityName the name of the city (path variable)
     * @return ResponseEntity with WeatherResponse and HTTP 200
     * @throws WeatherServiceException if service operation fails (handled by
     *                                 exception handler)
     */
    @GetMapping("/city/{cityName}")
    public ResponseEntity<WeatherResponse> getWeatherByCity(@PathVariable String cityName)
            throws WeatherServiceException {
        WeatherResponse response = weatherService.getWeatherByCity(cityName);
        return ResponseEntity.ok(response);
    }

    /**
     * Global exception handler for WeatherServiceException.
     * Maps error codes to appropriate HTTP status codes.
     *
     * @param ex the WeatherServiceException
     * @return ResponseEntity with ErrorResponse and appropriate HTTP status
     */
    @ExceptionHandler(WeatherServiceException.class)
    public ResponseEntity<ErrorResponse> handleWeatherServiceException(WeatherServiceException ex) {
        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Maps error codes to HTTP status codes.
     *
     * @param errorCode the error code from WeatherServiceException
     * @return appropriate HttpStatus
     */
    private HttpStatus mapErrorCodeToStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return switch (errorCode) {
            case "INVALID_INPUT" -> HttpStatus.BAD_REQUEST;
            case "CITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "API_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "API_TIMEOUT" -> HttpStatus.GATEWAY_TIMEOUT;
            case "UNEXPECTED_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

}
