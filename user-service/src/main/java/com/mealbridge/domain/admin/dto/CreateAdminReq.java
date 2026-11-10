package com.mealbridge.domain.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminReq(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 100) String password,
		@NotBlank @Size(max = 100) String name,
		@Size(max = 64) String department,
		@Size(max = 32) String phone,
		@Size(max = 500) String memo
) {
}
