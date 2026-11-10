package com.mealbridge.common.auth.dto;

public record EmailAvailabilityResponse(
		boolean validFormat,
		boolean available,
		String normalizedEmail
) {
}
