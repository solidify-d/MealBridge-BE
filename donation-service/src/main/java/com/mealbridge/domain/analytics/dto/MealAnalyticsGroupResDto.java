package com.mealbridge.domain.analytics.dto;

import java.math.BigDecimal;

public record MealAnalyticsGroupResDto(
		String groupKey,
		String groupLabel,
		int sessionCount,
		BigDecimal preparedKg,
		BigDecimal servedKg,
		BigDecimal wastedKg,
		BigDecimal wastePercent,
		Double avgTemperatureC,
		Integer avgHumidityPct) {
}
