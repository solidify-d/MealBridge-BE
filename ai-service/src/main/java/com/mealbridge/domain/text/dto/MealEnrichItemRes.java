package com.mealbridge.domain.text.dto;

import java.math.BigDecimal;
import java.util.List;

public record MealEnrichItemRes(
		String itemName,
		BigDecimal caloriesKcal,
		BigDecimal proteinG,
		BigDecimal carbsG,
		BigDecimal fatG,
		List<String> allergens,
		String allergyNotes,
		String summaryForNotes,
		String nutritionJson,
		String allergensJson
) {
}
