package com.mealbridge.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Auth/health paths including gateway-prefixed URIs ({@code /user-service/auth/...}).
 * Spring MVC double-wildcard path patterns (e.g. after {@code **}) are invalid; use this matcher instead.
 */
public final class PublicApiPathMatcher implements RequestMatcher {

	@Override
	public boolean matches(HttpServletRequest request) {
		return isPublicPath(request.getRequestURI());
	}

	public static boolean isPublicPath(String uri) {
		if (uri == null) {
			return false;
		}
		return isAuthPath(uri) || isHealthPath(uri);
	}

	private static boolean isAuthPath(String uri) {
		return uri.startsWith("/auth")
				|| uri.startsWith("/auth/")
				|| uri.contains("/auth/")
				|| uri.contains("/auth/register/");
	}

	private static boolean isHealthPath(String uri) {
		return "/health".equals(uri)
				|| uri.endsWith("/health")
				|| uri.contains("/health/");
	}
}
