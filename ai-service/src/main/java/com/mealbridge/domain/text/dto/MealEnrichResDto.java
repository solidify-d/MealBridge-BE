package com.mealbridge.domain.text.dto;

import java.util.List;

public record MealEnrichResDto(
		List<MealEnrichItemRes> items
) {
}
