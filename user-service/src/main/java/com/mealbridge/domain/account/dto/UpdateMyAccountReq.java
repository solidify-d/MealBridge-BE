package com.mealbridge.domain.account.dto;

import com.mealbridge.domain.donor.entity.DonorInstitutionType;
import com.mealbridge.domain.organization.entity.OrganizationType;
import com.mealbridge.domain.organization.entity.TargetDemographic;
import jakarta.validation.constraints.Size;

public record UpdateMyAccountReq(
		@Size(max = 100) String name,
		@Size(max = 32) String phone,
		@Size(max = 64) String department,
		@Size(max = 500) String memo,
		@Size(min = 8, max = 100) String newPassword,
		@Size(max = 200) String organizationName,
		OrganizationType organizationType,
		@Size(max = 500) String address,
		@Size(max = 32) String organizationPhone,
		@Size(max = 200) String serviceArea,
		Integer recipientCapacity,
		TargetDemographic targetDemographic,
		@Size(max = 64) String registrationNumber,
		@Size(max = 200) String institutionName,
		DonorInstitutionType institutionType,
		@Size(max = 32) String institutionPhone,
		@Size(max = 1000) String description,
		@Size(max = 64) String businessRegistrationNumber,
		@Size(max = 64) String unitIdentifier,
		Boolean removeProfileImage
) {
}
