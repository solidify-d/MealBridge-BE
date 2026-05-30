package com.mealbridge.common.auth.dto;

import com.mealbridge.domain.organization.OrganizationType;
import com.mealbridge.domain.organization.TargetDemographic;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrganizationRegisterRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 100) String password,
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 32) String phone,
		@NotBlank @Size(max = 200) String organizationName,
		@NotNull OrganizationType organizationType,
		@NotBlank @Size(max = 500) String address,
		@NotBlank @Size(max = 32) String organizationPhone,
		@Size(max = 200) String serviceArea,
		Integer recipientCapacity,
		TargetDemographic targetDemographic,
		@Size(max = 64) String registrationNumber
) {
}
