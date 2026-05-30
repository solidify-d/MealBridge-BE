package com.mealbridge.admin.dto;

import com.mealbridge.domain.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAdminStatusRequest(@NotNull UserStatus status) {
}
