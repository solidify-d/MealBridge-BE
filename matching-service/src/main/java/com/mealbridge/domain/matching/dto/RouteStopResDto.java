package com.mealbridge.domain.matching.dto;

import java.util.List;
import java.util.UUID;

public record RouteStopResDto(
		int sequence,
		UUID donationId,
		UUID donorInstitutionId,
		String donorName,
		String address,
		Double latitude,
		Double longitude,
		long distanceFromPreviousM,
		long durationFromPreviousS) {
}
