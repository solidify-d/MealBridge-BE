package com.mealbridge.domain.matching.dto;

import com.mealbridge.domain.matching.entity.PickupAssignmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivePickupAssignmentResDto(
		UUID id,
		UUID donationId,
		UUID organizationId,
		UUID routePlanId,
		PickupAssignmentStatus status,
		String menuName,
		LocalDateTime estimatedArrivalAt,
		LocalDateTime inTransitAt,
		LocalDateTime completedAt) {
}
