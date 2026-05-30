package com.mealbridge.domain.donation.dto;

import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealsession.dto.LineItemInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ManualDonationReq(
		@NotNull UUID donorInstitutionId,
		UUID mealSessionId,
		@NotNull MealType mealType,
		@NotBlank String menuName,
		@NotNull @DecimalMin("0.001") BigDecimal totalWeightKg,
		LocalDateTime servingEndedAt,
		LocalDateTime storageStartedAt,
		LocalDateTime pickupDeadlineAt,
		String notes,
		@Valid List<LineItemInput> lineItems
) {
}
