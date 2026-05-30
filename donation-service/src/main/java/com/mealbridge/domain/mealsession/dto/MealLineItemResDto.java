package com.mealbridge.domain.mealsession.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MealLineItemResDto(
		UUID id,
		String itemName,
		BigDecimal preparedKg,
		BigDecimal perPersonKg,
		BigDecimal remainingKg,
		String ingredients,
		String nutritionJson,
		String allergensJson
) {
}
