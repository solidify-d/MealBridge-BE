package com.mealbridge.domain.donor.dto;

import com.mealbridge.domain.common.entity.InstitutionStatus;
import com.mealbridge.domain.donor.entity.DonorInstitutionType;
import com.mealbridge.domain.user.entity.UserStatus;

import java.util.UUID;

public record DonorProfileResDto(
		UUID userId,
		String email,
		String name,
		String phone,
		UserStatus userStatus,
		UUID donorInstitutionId,
		String institutionName,
		DonorInstitutionType institutionType,
		String address,
		String institutionPhone,
		InstitutionStatus institutionStatus
) {
}
