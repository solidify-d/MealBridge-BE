package com.mealbridge.common.auth.dto;

import com.mealbridge.domain.user.Role;
import com.mealbridge.domain.user.UserStatus;

import java.util.UUID;

public record LoginResponse(
		UUID userId,
		String email,
		String name,
		Role role,
		UserStatus status
) {
}
