package com.mealbridge.admin.dto;

import com.mealbridge.domain.user.UserStatus;

import java.util.UUID;

public record AdminUserResponse(
		UUID id,
		String email,
		String name,
		UserStatus status,
		UUID createdById
) {
}
