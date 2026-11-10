package com.mealbridge.domain.weather.client;

import com.mealbridge.domain.weather.client.OpenMeteoDtos.ForecastResponse;
import com.mealbridge.domain.weather.client.OpenMeteoDtos.GeocodingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Component
public class OpenMeteoClient {

	private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
	private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

	private final RestTemplate openMeteoRestTemplate;

	public OpenMeteoClient(@Qualifier("openMeteoRestTemplate") RestTemplate openMeteoRestTemplate) {
		this.openMeteoRestTemplate = openMeteoRestTemplate;
	}

	public Optional<GeoPoint> geocode(String query) {
		if (query == null || query.isBlank()) {
			return Optional.empty();
		}

		String url = UriComponentsBuilder.fromHttpUrl(GEOCODING_URL)
				.queryParam("name", query.trim())
				.queryParam("count", 1)
				.queryParam("language", "en")
				.queryParam("format", "json")
				.toUriString();

		GeocodingResponse response = openMeteoRestTemplate.getForObject(url, GeocodingResponse.class);
		if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
			return Optional.empty();
		}

		var result = response.getResults().get(0);
		return Optional.of(new GeoPoint(result.getLatitude(), result.getLongitude(), result.getName()));
	}

	public Optional<ForecastResponse> fetchCurrentWeather(double latitude, double longitude) {
		String url = UriComponentsBuilder.fromHttpUrl(FORECAST_URL)
				.queryParam("latitude", latitude)
				.queryParam("longitude", longitude)
				.queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m")
				.queryParam("timezone", "Asia/Seoul")
				.toUriString();

		return Optional.ofNullable(openMeteoRestTemplate.getForObject(url, ForecastResponse.class));
	}

	public record GeoPoint(double latitude, double longitude, String label) {
	}
}
