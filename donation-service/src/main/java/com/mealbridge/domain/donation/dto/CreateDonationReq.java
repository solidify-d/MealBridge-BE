package com.mealbridge.domain.donation.dto;

import com.mealbridge.domain.donation.entity.MealType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateDonationReq(
		@NotNull UUID donorInstitutionId,
		@NotNull MealType mealType,
		@NotBlank String menuName,
		LocalDateTime servingEndedAt,
		LocalDateTime storageStartedAt,
		LocalDateTime pickupDeadlineAt,
		String notes
) {
}
