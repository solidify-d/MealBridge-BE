package com.mealbridge.domain.weather.dto;

import java.time.LocalDateTime;

public record SessionWeatherResDto(
		Double latitude,
		Double longitude,
		Double temperatureC,
		Integer humidityPct,
		Double precipitationMm,
		Double windSpeedKmh,
		Integer weatherCode,
		String weatherLabel,
		LocalDateTime capturedAt
) {
}
