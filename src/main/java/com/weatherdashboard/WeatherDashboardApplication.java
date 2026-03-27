package com.weatherdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Weather Dashboard.
 * Entry point for Spring Boot application.
 */
@SpringBootApplication(scanBasePackages = "com.weatherdashboard")
public class WeatherDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherDashboardApplication.class, args);
    }

}
