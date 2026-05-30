package com.mealbridge.domain.matching.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PickupPlanItemResDto(
		UUID donationId,
		String menuName,
		BigDecimal totalWeightKg,
		double score,
		Double distanceKm,
		Integer hoursUntilDeadline) {
}
