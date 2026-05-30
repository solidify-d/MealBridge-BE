package com.mealbridge.domain.mealsession.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LineItemSnapshot(
		@NotBlank String itemName,
		@NotNull @DecimalMin("0") BigDecimal kg
) {
}
