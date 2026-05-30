package com.mealbridge.domain.platformuser.dto;

import com.mealbridge.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePlatformUserStatusReq(
		@NotNull UserStatus status,
		@Size(max = 500) String reason
) {
}
