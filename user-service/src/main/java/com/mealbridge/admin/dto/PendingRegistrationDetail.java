package com.mealbridge.admin.dto;

import com.mealbridge.domain.donor.DonorInstitutionType;
import com.mealbridge.domain.organization.OrganizationType;
import com.mealbridge.domain.organization.TargetDemographic;
import com.mealbridge.domain.user.Role;
import com.mealbridge.domain.user.UserStatus;

import java.util.UUID;

public record PendingRegistrationDetail(
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
