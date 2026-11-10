package com.mealbridge.domain.mealsession.dto;

import java.math.BigDecimal;

public record HourlyTrendPoint(
		int hour,
		BigDecimal totalKg,
		int eventCount
) {
}
