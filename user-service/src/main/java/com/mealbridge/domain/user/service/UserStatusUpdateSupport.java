package com.mealbridge.domain.user.service;

import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;

public final class UserStatusUpdateSupport {

	private UserStatusUpdateSupport() {
	}

	public static void applyStatus(User user, UserStatus status, String reason) {
		if (status == UserStatus.SUSPENDED) {
			if (reason == null || reason.isBlank()) {
				throw new IllegalArgumentException("Suspension reason is required");
			}
			user.updateStatus(UserStatus.SUSPENDED, reason.trim());
			return;
		}
		user.updateStatus(status);
	}
}
