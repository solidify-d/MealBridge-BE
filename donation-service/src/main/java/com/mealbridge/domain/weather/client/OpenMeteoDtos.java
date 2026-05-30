package com.mealbridge.domain.weather.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

public final class OpenMeteoDtos {

	private OpenMeteoDtos() {
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GeocodingResponse {
		private List<GeocodingResult> results;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class GeocodingResult {
		private double latitude;
		private double longitude;
		private String name;
		private String country;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ForecastResponse {
		private CurrentWeather current;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CurrentWeather {
		private String time;
		@JsonProperty("temperature_2m")
		private Double temperature2m;
		@JsonProperty("relative_humidity_2m")
		private Integer relativeHumidity2m;
		private Double precipitation;
		@JsonProperty("wind_speed_10m")
		private Double windSpeed10m;
		@JsonProperty("weather_code")
		private Integer weatherCode;
	}
}
