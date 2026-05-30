package com.mealbridge.common.auth;

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
		UUID userId = requireUserId(userHeader);
		requireRole(roleHeader);
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new SecurityException("User not found"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new SecurityException("Account is not active");
		}
		return userId;
	}

	public void requireSuperAdmin(String roleHeader) {
		String role = requireRole(roleHeader);
		if (!role.equalsIgnoreCase("SUPER_ADMIN")) {
			throw new SecurityException("SUPER_ADMIN role is required");
		}
	}

	public void requireAdmin(String roleHeader) {
		String role = requireRole(roleHeader);
		if (!role.equalsIgnoreCase("SUPER_ADMIN") && !role.equalsIgnoreCase("ADMIN")) {
			throw new SecurityException("ADMIN role is required");
		}
	}

	public UUID requireOrganization(String userHeader, String roleHeader) {
		requireActiveUser(userHeader, roleHeader);
		if (!roleHeader.equalsIgnoreCase("ORGANIZATION")) {
			throw new SecurityException("ORGANIZATION role is required");
		}
		return requireUserId(userHeader);
	}

	public UUID requireDonor(String userHeader, String roleHeader) {
		requireActiveUser(userHeader, roleHeader);
		if (!roleHeader.equalsIgnoreCase("DONOR")) {
			throw new SecurityException("DONOR role is required");
		}
		return requireUserId(userHeader);
	}
}
