package com.mealbridge.common.security;

import com.mealbridge.domain.user.Role;
import com.mealbridge.domain.user.UserStatus;

import java.util.UUID;

public record CustomUserPrincipal(UUID userId, Role role, UserStatus status) {
}
