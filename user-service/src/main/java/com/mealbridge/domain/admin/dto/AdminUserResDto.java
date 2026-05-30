package com.mealbridge.domain.admin.dto;

import com.mealbridge.domain.user.entity.UserStatus;

import java.util.UUID;

public record AdminUserResDto(
		UUID id,
		String email,
		String name,
		String phone,
		String department,
		String memo,
		UserStatus status,
		String suspendReason,
		UUID createdById
) {
}
