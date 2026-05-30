package com.mealbridge.common.auth.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mealbridge.email-verification")
public record EmailVerificationProperties(
		boolean enabled,
		int codeLength,
		int codeTtlMinutes,
		int verifiedTtlMinutes,
		int resendCooldownSeconds,
		String fromAddress,
		String fromName,
		String devFixedCode
) {
	public EmailVerificationProperties {
		if (codeLength <= 0) {
			codeLength = 6;
		}
		if (codeTtlMinutes <= 0) {
			codeTtlMinutes = 10;
		}
		if (verifiedTtlMinutes <= 0) {
			verifiedTtlMinutes = 30;
		}
		if (resendCooldownSeconds <= 0) {
			resendCooldownSeconds = 60;
		}
		if (fromName == null || fromName.isBlank()) {
			fromName = "ConnectMeal";
		}
	}
}
