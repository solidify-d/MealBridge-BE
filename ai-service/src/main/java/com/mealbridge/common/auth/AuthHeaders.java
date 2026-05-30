package com.mealbridge.common.auth;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class AuthHeaders {

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

	public UUID requireDonor(String userHeader, String roleHeader) {
		UUID userId = requireUserId(userHeader);
		String role = requireRole(roleHeader);
		if (!role.equalsIgnoreCase("DONOR")) {
			throw new SecurityException("DONOR role is required");
		}
		return userId;
	}
}
