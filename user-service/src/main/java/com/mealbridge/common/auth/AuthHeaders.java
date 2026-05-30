package com.mealbridge.common.auth;

import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthHeaders {

	private final UserRepository userRepository;

	public UUID requireUserId(String userHeader) {
		if (!StringUtils.hasText(userHeader)) {
			throw new SecurityException("X-User-Id header is required");
		}
		try {
			return UUID.fromString(userHeader.trim());
		} catch (IllegalArgumentException ex) {
			throw new SecurityException("Invalid X-User-Id format");
		}
	}

	public String requireRole(String roleHeader) {
		if (!StringUtils.hasText(roleHeader)) {
			throw new SecurityException("X-User-Role header is required");
		}
		return roleHeader.trim();
	}

	public UUID requireActiveUser(String userHeader, String roleHeader) {
		return loadActiveUser(userHeader, roleHeader, null).getId();
	}

	public UUID requireActiveSuperAdmin(String userHeader, String roleHeader) {
		return loadActiveUser(userHeader, roleHeader, Role.SUPER_ADMIN).getId();
	}

	public UUID requireActiveAdmin(String userHeader, String roleHeader) {
		return loadActiveUser(userHeader, roleHeader, Role.ADMIN).getId();
	}

	public UUID requireOrganization(String userHeader, String roleHeader) {
		return loadActiveUser(userHeader, roleHeader, Role.ORGANIZATION).getId();
	}

	public UUID requireDonor(String userHeader, String roleHeader) {
		return loadActiveUser(userHeader, roleHeader, Role.DONOR).getId();
	}

	private User loadActiveUser(String userHeader, String roleHeader, Role requiredRole) {
		UUID userId = requireUserId(userHeader);
		String role = requireRole(roleHeader);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new SecurityException("User not found"));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new SecurityException("Account is not active");
		}

		if (!user.getRole().name().equalsIgnoreCase(role)) {
			throw new SecurityException("Role mismatch");
		}

		if (requiredRole != null && user.getRole() != requiredRole) {
			throw new SecurityException(requiredRole.name() + " role is required");
		}

		return user;
	}
}
