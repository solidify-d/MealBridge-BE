package com.mealbridge.common.s3;

import com.mealbridge.domain.user.entity.Role;

import java.util.UUID;

/**
 * S3 object key prefixes under the bucket root folder {@value #ROOT}.
 *
 * <pre>
 * {bucket}/
 *   MealConnect/
 *     profiles/
 *       organization/{userId}/{uuid}-{filename}
 *       donor/{userId}/{uuid}-{filename}
 *       admin/{userId}/{uuid}-{filename}
 * </pre>
 */
public final class MealConnectS3Paths {

	public static final String ROOT = "MealConnect";

	private MealConnectS3Paths() {
	}

	public static String profileImage(Role role, UUID userId) {
		return String.format("%s/profiles/%s/%s", ROOT, profileRoleFolder(role), userId);
	}

	private static String profileRoleFolder(Role role) {
		return switch (role) {
			case ORGANIZATION -> "organization";
			case DONOR -> "donor";
			case ADMIN, SUPER_ADMIN -> "admin";
		};
	}
}
