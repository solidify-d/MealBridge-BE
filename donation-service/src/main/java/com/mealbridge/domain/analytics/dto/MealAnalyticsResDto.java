package com.mealbridge.domain.analytics.dto;

import java.util.List;

public record MealAnalyticsResDto(
		String groupBy,
		List<MealAnalyticsGroupResDto> groups) {
}
