package com.mealbridge.common.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {

	private CookieOption access = new CookieOption();
	private CookieOption refresh = new CookieOption();
	private CookieOption csrf = new CookieOption();

	@Getter
	@Setter
	public static class CookieOption {
		private boolean httpOnly;
		private boolean secure;
		private String sameSite;
		private String path;
		private String domain;
	}
}
