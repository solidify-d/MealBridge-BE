package com.mealbridge.domain.platformuser.dto;

import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlatformUserSummaryResDto(
		UUID userId,
		String email,
		String name,
		String phone,
		Role role,
		UserStatus status,
		String institutionName,
		LocalDateTime createdAt
) {
}
