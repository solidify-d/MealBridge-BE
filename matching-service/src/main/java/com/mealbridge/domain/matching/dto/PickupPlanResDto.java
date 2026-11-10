package com.mealbridge.domain.matching.dto;

import java.util.List;
import java.util.UUID;

public record PickupPlanResDto(
		UUID organizationId,
		List<PickupPlanItemResDto> selectedMeals,
		OptimizedRouteResDto route,
		long roundTripDurationS,
		String servingStartTime,
		String summary,
		boolean canConfirm) {
}
