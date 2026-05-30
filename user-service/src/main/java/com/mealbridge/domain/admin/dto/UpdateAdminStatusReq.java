package com.mealbridge.domain.admin.dto;

import com.mealbridge.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAdminStatusReq(@NotNull UserStatus status) {
}
