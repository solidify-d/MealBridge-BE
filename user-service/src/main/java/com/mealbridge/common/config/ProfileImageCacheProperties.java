package com.mealbridge.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mealbridge.profile-image")
public record ProfileImageCacheProperties(
		/** Redis TTL for profileimg:{userId} — default 7 days */
		int cacheTtlDays
) {
	public ProfileImageCacheProperties {
		if (cacheTtlDays <= 0) {
			cacheTtlDays = 7;
		}
	}
}
