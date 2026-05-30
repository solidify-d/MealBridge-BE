package com.mealbridge.domain.registration.dto;

import com.mealbridge.domain.donor.entity.DonorInstitutionType;
import com.mealbridge.domain.organization.entity.OrganizationType;
import com.mealbridge.domain.organization.entity.TargetDemographic;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.UserStatus;

import java.util.UUID;

public record PendingRegistrationDetailResDto(
		UUID userId,
		String email,
		String contactName,
		String contactPhone,
		Role role,
		UserStatus status,
		OrganizationDetail organization,
		DonorDetail donor
) {
	public record OrganizationDetail(
			UUID id,
			String name,
			OrganizationType type,
			String address,
			String organizationPhone,
			String serviceArea,
			Integer recipientCapacity,
			TargetDemographic targetDemographic,
			String registrationNumber
	) {
	}

	public record DonorDetail(
			UUID id,
			String name,
			DonorInstitutionType type,
			String address,
			String institutionPhone,
			String description,
			String businessRegistrationNumber,
			String unitIdentifier
	) {
	}
}
