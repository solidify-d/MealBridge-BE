package com.mealbridge.domain.matching.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RecommendationResDto(
		UUID organizationId,
		String organizationName,
		UUID donationId,
		String donationMenuName,
		BigDecimal totalWeightKg,
		double score,
		Double distanceKm,
		Integer hoursUntilDeadline) {
}
