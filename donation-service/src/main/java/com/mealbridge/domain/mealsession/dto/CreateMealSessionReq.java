package com.mealbridge.domain.mealsession.dto;

import com.mealbridge.domain.donation.entity.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateMealSessionReq(
		@NotNull UUID donorInstitutionId,
		@NotNull MealType mealType,
		String menuName,
		@NotNull LocalDate sessionDate,
		@DecimalMin("0.001") BigDecimal preparedWeightKg,
		@NotNull @Min(1) Integer headcount,
		String notes,
		@NotEmpty @Valid List<LineItemInput> lineItems,
		UUID mealPlanId,
		LocalDateTime expectedServingEndAt
) {
}
