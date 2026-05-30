package com.mealbridge.domain.organization.dto;

import com.mealbridge.domain.common.entity.InstitutionStatus;
import com.mealbridge.domain.organization.entity.OrganizationType;
import com.mealbridge.domain.organization.entity.TargetDemographic;
import com.mealbridge.domain.user.entity.UserStatus;

import java.util.UUID;

public record OrganizationProfileResDto(
		UUID userId,
		String email,
		String name,
		String phone,
		UserStatus userStatus,
		String profileImageUrl,
		UUID organizationId,
		String organizationName,
		OrganizationType organizationType,
		String address,
		String organizationPhone,
		Integer recipientCapacity,
		TargetDemographic targetDemographic,
		InstitutionStatus organizationStatus
) {
}
