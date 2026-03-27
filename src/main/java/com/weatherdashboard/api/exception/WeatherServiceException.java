package com.weatherdashboard.api.exception;

/**
 * Exception thrown for business logic errors in the WeatherService.
 * Wraps errors from external API or validation failures.
 */
public class WeatherServiceException extends Exception {
    private final String errorCode;
    private final String message;
    private final Throwable cause;

    /**
     * Constructs a WeatherServiceException with error code and cause.
     *
     * @param message   error message for client (non-technical)
     * @param errorCode machine-readable error code (e.g., "INVALID_INPUT",
     *                  "CITY_NOT_FOUND")
     * @param cause     the underlying exception
     */
    public WeatherServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.errorCode = errorCode;
        this.cause = cause;
    }

    public String getErrorCode() {
        return errorCode;
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
