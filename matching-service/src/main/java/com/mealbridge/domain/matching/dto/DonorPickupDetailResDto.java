package com.mealbridge.domain.matching.dto;

import com.mealbridge.domain.matching.entity.PickupAssignmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DonorPickupDetailResDto(
		UUID assignmentId,
		UUID organizationId,
		String organizationName,
		String organizationPhone,
		String contactPersonName,
		PickupAssignmentStatus assignmentStatus,
		LocalDateTime acceptedAt,
		LocalDateTime estimatedArrivalAt,
		LocalDateTime inTransitAt,
		LocalDateTime completedAt,
		LocalDateTime cancelledAt,
		String cancelReason) {
}
