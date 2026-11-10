package com.mealbridge.domain.matching.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SuggestPickupPlanReq(
		@NotNull UUID organizationId,
		Double maxPickupDistanceKm,
		Double desiredPickupWeightKg,
		String servingStartTime,
		List<UUID> donationIds) {
}
