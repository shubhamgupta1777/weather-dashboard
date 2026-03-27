package com.weatherdashboard.api.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenWeatherMap API response DTO.
 * Maps the external API structure for deserialization.
 * INTERNAL TO CLIENT LAYER - Never exposed above this layer.
 */
public class OpenWeatherMapResponse {
    private String name;
    private Main main;
    private Weather[] weather;
    private Wind wind;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("main")
    public Main getMain() {
        return main;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    @JsonProperty("weather")
    public Weather[] getWeather() {
        return weather;
    }

    public void setWeather(Weather[] weather) {
        this.weather = weather;
    }

    @JsonProperty("wind")
    public Wind getWind() {
        return wind;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    /**
     * Nested Main object containing temperature and humidity
     */
    public static class Main {
        private Double temp;
        private Integer humidity;

        @JsonProperty("temp")
        public Double getTemp() {
            return temp;
        }

        public void setTemp(Double temp) {
            this.temp = temp;
        }

        @JsonProperty("humidity")
        public Integer getHumidity() {
            return humidity;
        }

        public void setHumidity(Integer humidity) {
            this.humidity = humidity;
        }
    }

    /**
     * Nested Weather object containing weather condition
     */
    public static class Weather {
        private String main;

        @JsonProperty("main")
        public String getMain() {
            return main;
        }

        public void setMain(String main) {
            this.main = main;
        }
    }

    /**
     * Nested Wind object containing wind speed
     */
    public static class Wind {
        private Double speed;

        @JsonProperty("speed")
        public Double getSpeed() {
            return speed;
        }

        public void setSpeed(Double speed) {
            this.speed = speed;
        }
    }

}
