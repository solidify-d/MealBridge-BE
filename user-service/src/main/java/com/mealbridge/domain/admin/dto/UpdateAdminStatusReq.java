package com.mealbridge.domain.admin.dto;

import com.mealbridge.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAdminStatusReq(
		@NotNull UserStatus status,
		@Size(max = 500) String reason
) {
}
