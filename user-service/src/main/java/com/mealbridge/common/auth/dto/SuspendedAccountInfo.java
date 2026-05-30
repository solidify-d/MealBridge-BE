package com.mealbridge.common.auth.dto;

public record SuspendedAccountInfo(
		String reason,
		String contactEmail
) {
}
