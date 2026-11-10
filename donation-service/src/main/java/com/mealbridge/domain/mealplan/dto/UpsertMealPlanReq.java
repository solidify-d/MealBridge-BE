package com.mealbridge.domain.mealplan.dto;

import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealsession.dto.LineItemInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpsertMealPlanReq(
		@NotNull UUID donorInstitutionId,
		@NotNull LocalDate planDate,
		@NotNull MealType mealType,
		@NotNull @Min(1) Integer headcount,
		@NotEmpty @Valid List<LineItemInput> lineItems,
		String notes
) {
}
