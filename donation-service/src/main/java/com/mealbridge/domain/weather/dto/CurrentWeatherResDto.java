package com.mealbridge.domain.weather.dto;

import java.time.LocalDateTime;

public record CurrentWeatherResDto(
		String institutionName,
		String address,
		Double latitude,
		Double longitude,
		Double temperatureC,
		Integer humidityPct,
		Double precipitationMm,
		Double windSpeedKmh,
		Integer weatherCode,
		String weatherLabel,
		LocalDateTime observedAt
) {
}
