package com.mealbridge.domain.mealsession.dto;

import java.math.BigDecimal;

/** Single-day bucket: {@code hour} 0–23. */
public record MealTypeHourlyPoint(
		int hour,
		BigDecimal breakfastKg,
		BigDecimal lunchKg,
		BigDecimal dinnerKg
) {
}
