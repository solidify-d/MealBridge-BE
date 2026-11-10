package com.mealbridge.domain.donation.dto;

import com.mealbridge.domain.donation.entity.DonationStatus;
import com.mealbridge.domain.donation.entity.MealType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonationResDto(
		UUID id,
		UUID donorInstitutionId,
		UUID registeredByUserId,
		DonationStatus status,
		MealType mealType,
		String menuName,
		LocalDateTime servingEndedAt,
		LocalDateTime storageStartedAt,
		LocalDateTime pickupDeadlineAt,
		String beforeImageUrl,
		String afterImageUrl,
		String notes,
		UUID mealSessionId,
		BigDecimal totalWeightKg,
		String lineItemsJson,
		UUID acceptedOrganizationId,
		LocalDateTime acceptedAt,
		LocalDateTime inTransitAt,
		LocalDateTime completedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
