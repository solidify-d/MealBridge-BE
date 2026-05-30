package com.mealbridge.domain.mealplan.dto;

import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealsession.dto.LineItemInput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MealPlanResDto(
		UUID id,
		UUID donorInstitutionId,
		LocalDate planDate,
		MealType mealType,
		BigDecimal preparedWeightKg,
		Integer headcount,
		List<LineItemInput> lineItems,
		String notes,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
