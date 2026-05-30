package com.mealbridge.common.security;

import com.mealbridge.domain.user.UserRepository;
import com.mealbridge.domain.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Method-security guard (NylonMusk {@code AdminAccessGuard} pattern).
 * Used by {@link SuperAdminOnly}, {@link AdminOnly}, etc.
 */
@Component("userAccessGuard")
@RequiredArgsConstructor
public class UserAccessGuard {

	private final UserRepository userRepository;

	public boolean isActiveUser(UUID userId) {
		return userRepository.findById(userId)
				.map(user -> user.getStatus() == UserStatus.ACTIVE)
				.orElse(false);
	}

	public boolean hasActiveRole(UUID userId, String role) {
		return userRepository.findById(userId)
				.map(user -> user.getStatus() == UserStatus.ACTIVE
						&& user.getRole().name().equalsIgnoreCase(role))
				.orElse(false);
	}
}
