package com.mealbridge.admin.dto;

import com.mealbridge.domain.user.Role;
import com.mealbridge.domain.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record PendingRegistrationSummary(
		UUID userId,
		String email,
		String contactName,
		String contactPhone,
		Role role,
		UserStatus status,
		String institutionName,
		Instant createdAt
) {
}
