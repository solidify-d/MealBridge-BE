package com.mealbridge.domain.mealsession.dto;

import java.math.BigDecimal;

public record MealItemBreakdown(
		String itemName,
		BigDecimal preparedKg,
		BigDecimal servedKg,
		BigDecimal wastedKg,
		BigDecimal wastePercent,
		String ingredients
) {
}
