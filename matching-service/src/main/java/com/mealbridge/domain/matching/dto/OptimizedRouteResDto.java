package com.mealbridge.domain.matching.dto;

import java.util.List;
import java.util.UUID;

public record OptimizedRouteResDto(
		UUID routePlanId,
		UUID organizationId,
		List<RouteStopResDto> stops,
		long totalDistanceM,
		long totalDurationS) {
}
