package com.mealbridge.domain.text.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MealEnrichItemReq(
		@NotBlank String itemName,
		String ingredients,
		@NotNull @Positive BigDecimal preparedKg
) {
}
