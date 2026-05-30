package com.mealbridge.common.auth.dto;

public record EmailVerificationResponse(
		String message,
		boolean verified
) {
	public static EmailVerificationResponse sent(String message) {
		return new EmailVerificationResponse(message, false);
	}

	public static EmailVerificationResponse confirmed(String message) {
		return new EmailVerificationResponse(message, true);
	}
}
