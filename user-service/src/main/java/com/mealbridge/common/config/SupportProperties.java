package com.mealbridge.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mealbridge.support")
public record SupportProperties(
		String contactEmail
) {
}
