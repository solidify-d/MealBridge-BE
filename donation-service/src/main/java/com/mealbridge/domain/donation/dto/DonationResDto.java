package com.mealbridge.domain.donation.dto;

import com.mealbridge.domain.donation.entity.DonationStatus;
import com.mealbridge.domain.donation.entity.MealType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DonationResDto(
		UUID id,
		UUID organizationId,
		UUID registeredByUserId,
		DonationStatus status,
		MealType mealType,
		String menuName,
		LocalDateTime servingEndedAt,
		LocalDateTime storageStartedAt,
		String beforeImageUrl,
		String afterImageUrl,
		String notes,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
