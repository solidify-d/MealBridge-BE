package com.mealbridge.domain.matching.dto;

import com.mealbridge.domain.matching.entity.PickupAssignmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrganizationPickupHistoryResDto(
		UUID assignmentId,
		UUID donationId,
		UUID organizationId,
		UUID routePlanId,
		PickupAssignmentStatus assignmentStatus,
		String donationStatus,
		String menuName,
		String mealType,
		BigDecimal totalWeightKg,
		String notes,
		String beforeImageUrl,
		String afterImageUrl,
		List<OrganizationPickupHistoryLineItemResDto> lineItems,
		UUID donorInstitutionId,
		String donorName,
		String donorAddress,
		UUID mealSessionId,
		boolean sessionBulkPickup,
		double score,
		LocalDateTime estimatedArrivalAt,
		LocalDateTime inTransitAt,
		LocalDateTime completedAt,
		LocalDateTime cancelledAt,
		String cancelReason,
		LocalDateTime acceptedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
