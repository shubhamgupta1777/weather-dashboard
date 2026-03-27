package com.weatherdashboard.api.dto;

/**
 * Response DTO for error information.
 * Used in HTTP error responses to clients.
 */
public record ErrorResponse(
        String error) {
}
