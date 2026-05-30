package com.mealbridge.domain.mealsession.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealSessionBreakdown(
		LocalDate sessionDate,
		String menuName,
		Integer headcount,
		BigDecimal preparedKg,
		BigDecimal servedKg,
		BigDecimal wastedKg,
		List<MealItemBreakdown> items
) {
}
