package com.mealbridge.domain.weather.service;

import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.mealsession.entity.MealSession;
import com.mealbridge.domain.weather.client.DonorInstitutionClient;
import com.mealbridge.domain.weather.client.DonorInstitutionClient.InstitutionLocation;
import com.mealbridge.domain.weather.client.OpenMeteoClient;
import com.mealbridge.domain.weather.client.OpenMeteoClient.GeoPoint;
import com.mealbridge.domain.weather.client.OpenMeteoDtos.ForecastResponse;
import com.mealbridge.domain.weather.dto.CurrentWeatherResDto;
import com.mealbridge.domain.weather.dto.SessionWeatherResDto;
import com.mealbridge.domain.weather.support.WeatherLabels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

	private final DonorInstitutionClient donorInstitutionClient;
	private final OpenMeteoClient openMeteoClient;

	public CurrentWeatherResDto getCurrentWeather(UUID donorInstitutionId) {
		InstitutionLocation location = donorInstitutionClient.fetchLocation(donorInstitutionId);
		GeoPoint point = resolveGeoPoint(location);
		ForecastResponse forecast = openMeteoClient.fetchCurrentWeather(point.latitude(), point.longitude())
				.orElseThrow(() -> new IllegalStateException("Weather data is unavailable right now"));

		var current = forecast.getCurrent();
		LocalDateTime observedAt = parseObservedAt(current.getTime());

		return new CurrentWeatherResDto(
				location.name(),
				location.address(),
				point.latitude(),
				point.longitude(),
				current.getTemperature2m(),
				current.getRelativeHumidity2m(),
				current.getPrecipitation(),
				current.getWindSpeed10m(),
				current.getWeatherCode(),
				WeatherLabels.labelForCode(current.getWeatherCode()),
				observedAt);
	}

	public void attachWeatherSnapshot(MealSession session) {
		try {
			InstitutionLocation location = donorInstitutionClient.fetchLocation(session.getDonorInstitutionId());
			GeoPoint point = resolveGeoPoint(location);
			ForecastResponse forecast = openMeteoClient.fetchCurrentWeather(point.latitude(), point.longitude())
					.orElse(null);
			if (forecast == null || forecast.getCurrent() == null) {
				log.warn("Skipping weather snapshot for session {} — Open-Meteo returned no data", session.getId());
				return;
			}

			var current = forecast.getCurrent();
			LocalDateTime capturedAt = parseObservedAt(current.getTime());
			if (capturedAt == null) {
				capturedAt = KstDates.now();
			}

			session.applyWeatherSnapshot(
					point.latitude(),
					point.longitude(),
					current.getTemperature2m(),
					current.getRelativeHumidity2m(),
					current.getPrecipitation(),
					current.getWindSpeed10m(),
					current.getWeatherCode(),
					capturedAt,
					JsonUtil.toJson(java.util.Map.ofEntries(
							java.util.Map.entry("source", "open-meteo"),
							java.util.Map.entry("institutionName", location.name()),
							java.util.Map.entry("address", location.address()),
							java.util.Map.entry("latitude", point.latitude()),
							java.util.Map.entry("longitude", point.longitude()),
							java.util.Map.entry("temperature2m", current.getTemperature2m()),
							java.util.Map.entry("relativeHumidity2m", current.getRelativeHumidity2m()),
							java.util.Map.entry("precipitation", current.getPrecipitation()),
							java.util.Map.entry("windSpeed10m", current.getWindSpeed10m()),
							java.util.Map.entry("weatherCode", current.getWeatherCode()),
							java.util.Map.entry("observedAt", current.getTime()))));
		} catch (RuntimeException ex) {
			log.warn("Failed to attach weather snapshot for session {}: {}", session.getId(), ex.getMessage());
		}
	}

	public SessionWeatherResDto toSessionWeatherDto(MealSession session) {
		if (session.getWeatherCapturedAt() == null) {
			return null;
		}
		return new SessionWeatherResDto(
				session.getWeatherLatitude(),
				session.getWeatherLongitude(),
				session.getWeatherTemperatureC(),
				session.getWeatherHumidityPct(),
				session.getWeatherPrecipitationMm(),
				session.getWeatherWindSpeedKmh(),
				session.getWeatherCode(),
				WeatherLabels.labelForCode(session.getWeatherCode()),
				session.getWeatherCapturedAt());
	}

	private GeoPoint resolveGeoPoint(InstitutionLocation location) {
		if (location.latitude() != null && location.longitude() != null) {
			return new GeoPoint(location.latitude(), location.longitude(), location.name());
		}

		String query = buildGeocodeQuery(location.address(), location.name());
		return openMeteoClient.geocode(query)
				.orElseThrow(() -> new IllegalArgumentException(
						"Could not resolve institution location for weather lookup"));
	}

	private String buildGeocodeQuery(String address, String institutionName) {
		if (address != null && !address.isBlank()) {
			String cleaned = address.replaceAll("^\\(\\d{5}\\)\\s*", "").trim();
			if (!cleaned.isBlank()) {
				return cleaned + ", South Korea";
			}
		}
		if (institutionName != null && !institutionName.isBlank()) {
			return institutionName + ", South Korea";
		}
		return "Seoul, South Korea";
	}

	private LocalDateTime parseObservedAt(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return OffsetDateTime.parse(value).toLocalDateTime();
		} catch (RuntimeException ignored) {
			try {
				return LocalDateTime.parse(value);
			} catch (RuntimeException ex) {
				return null;
			}
		}
	}
}
