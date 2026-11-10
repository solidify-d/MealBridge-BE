package com.mealbridge.domain.account.dto;

import com.mealbridge.domain.common.entity.InstitutionStatus;
import com.mealbridge.domain.donor.entity.DonorInstitutionType;
import com.mealbridge.domain.donor.entity.PickupAcceptanceMode;
import com.mealbridge.domain.organization.entity.OrganizationType;
import com.mealbridge.domain.organization.entity.TargetDemographic;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.UserStatus;

import java.util.UUID;

public record MyAccountProfileResDto(
		UUID userId,
		String email,
		String name,
		String phone,
		Role role,
		UserStatus userStatus,
		String profileImageUrl,
		String department,
		String memo,
		UUID organizationId,
		String organizationName,
		OrganizationType organizationType,
		String address,
		String organizationPhone,
		String serviceArea,
		Integer recipientCapacity,
		TargetDemographic targetDemographic,
		String registrationNumber,
		InstitutionStatus organizationStatus,
		UUID donorInstitutionId,
		String institutionName,
		DonorInstitutionType institutionType,
		String institutionPhone,
		String description,
		String businessRegistrationNumber,
		String unitIdentifier,
		InstitutionStatus institutionStatus,
		Double institutionLatitude,
		Double institutionLongitude,
		Double organizationLatitude,
		Double organizationLongitude,
		Double maxPickupDistanceKm,
		Double desiredPickupWeightKg,
		String servingStartTime,
		PickupAcceptanceMode pickupAcceptanceMode
) {
}
