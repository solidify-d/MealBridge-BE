package com.mealbridge.common.auth.dto;

import com.mealbridge.domain.organization.entity.OrganizationType;
import com.mealbridge.domain.organization.entity.TargetDemographic;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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
		@NotBlank @Size(max = 200) String serviceArea,
		@NotNull @Min(1) Integer recipientCapacity,
		@NotNull TargetDemographic targetDemographic,
		@NotBlank @Size(max = 64) String registrationNumber
) {
}
