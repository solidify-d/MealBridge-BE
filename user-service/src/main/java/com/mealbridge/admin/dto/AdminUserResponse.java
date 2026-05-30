package com.mealbridge.admin.dto;

import com.mealbridge.domain.user.UserStatus;

import java.util.UUID;

public record AdminUserResponse(
		UUID id,
		String email,
		String name,
		String phone,
		String department,
		String memo,
		UserStatus status,
		UUID createdById
) {
}
