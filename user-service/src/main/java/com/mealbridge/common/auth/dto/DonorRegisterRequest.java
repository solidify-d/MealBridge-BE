package com.mealbridge.common.auth.dto;

import com.mealbridge.domain.donor.entity.DonorInstitutionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonorRegisterRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 100) String password,
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 32) String phone,
		@NotBlank @Size(max = 200) String institutionName,
		@NotNull DonorInstitutionType institutionType,
		@NotBlank @Size(max = 500) String address,
		@NotBlank @Size(max = 32) String institutionPhone,
		@Size(max = 1000) String description,
		@Size(max = 64) String businessRegistrationNumber,
		@Size(max = 64) String unitIdentifier
) {
}
