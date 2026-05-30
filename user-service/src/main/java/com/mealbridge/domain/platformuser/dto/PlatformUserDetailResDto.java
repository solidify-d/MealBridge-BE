package com.mealbridge.domain.platformuser.dto;

import com.mealbridge.domain.registration.dto.PendingRegistrationDetailResDto;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlatformUserDetailResDto(
		UUID userId,
		String email,
		String contactName,
		String contactPhone,
		Role role,
		UserStatus status,
		String suspendReason,
		LocalDateTime createdAt,
		String department,
		String memo,
		PendingRegistrationDetailResDto.OrganizationDetail organization,
		PendingRegistrationDetailResDto.DonorDetail donor
) {
}
