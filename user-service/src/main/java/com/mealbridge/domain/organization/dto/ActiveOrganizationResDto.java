package com.mealbridge.domain.organization.dto;

import java.util.UUID;

public record ActiveOrganizationResDto(
		UUID id,
		String name,
		String address,
		Double latitude,
		Double longitude,
		Integer recipientCapacity,
		String serviceArea,
		String targetDemographic,
		Double maxPickupDistanceKm,
		Double desiredPickupWeightKg,
		String servingStartTime) {
}
