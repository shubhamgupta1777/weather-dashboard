package com.weatherdashboard.api.exception;

/**
 * Exception thrown when external weather API calls fail.
 * Used by WeatherApiClient to wrap HTTP and network errors.
 */
public class ExternalApiException extends Exception {
    private final int statusCode;
    private final String message;
    private final Throwable cause;

    /**
     * Constructs an ExternalApiException with status code and cause.
     *
     * @param message    error message
     * @param statusCode HTTP status code (0 indicates timeout/network error)
     * @param cause      the underlying exception
     */
    public ExternalApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.statusCode = statusCode;
        this.cause = cause;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

}
