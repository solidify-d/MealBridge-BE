package com.mealbridge.common.s3;

import java.util.UUID;

public final class MealConnectS3Paths {

	public static final String ROOT = "MealConnect";

	private MealConnectS3Paths() {
	}

	public static String donation(UUID donorInstitutionId, UUID donationId) {
		return String.format("%s/donations/%s/%s", ROOT, donorInstitutionId, donationId);
	}
}
