package com.mealbridge.domain.matching.dto;

import com.mealbridge.domain.matching.entity.PickupAssignmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PickupAssignmentResDto(
		UUID id,
		UUID donationId,
		UUID organizationId,
		PickupAssignmentStatus status,
		double score,
		LocalDateTime createdAt) {
}
