package com.mealbridge.domain.mealsession.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MealTypeDailyPoint(
		LocalDate date,
		BigDecimal breakfastKg,
		BigDecimal lunchKg,
		BigDecimal dinnerKg
) {
}
