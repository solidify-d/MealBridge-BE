package com.mealbridge.domain.mealsession.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LineItemInput(
		@NotBlank String itemName,
		@NotNull @DecimalMin("0.001") BigDecimal preparedKg,
		@DecimalMin("0") BigDecimal perPersonKg,
		String ingredients,
		String nutritionJson,
		String allergensJson
) {
}
