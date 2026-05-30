package com.mealbridge.domain.registration.dto;

import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingRegistrationSummaryResDto(
		UUID userId,
		String email,
		String contactName,
		String contactPhone,
		Role role,
		UserStatus status,
		String institutionName,
		LocalDateTime createdAt
) {
}
