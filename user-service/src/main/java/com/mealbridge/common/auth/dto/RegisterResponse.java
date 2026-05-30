package com.mealbridge.common.auth.dto;

import com.mealbridge.domain.user.UserStatus;

import java.util.UUID;

public record RegisterResponse(
		UUID userId,
		UserStatus status,
		String message
) {
}
