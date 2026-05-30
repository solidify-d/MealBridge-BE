package com.mealbridge.common.auth.service;

import com.mealbridge.common.auth.config.AuthCookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

	private final AuthCookieProperties cookieProps;
	private final JwtService jwtService;

	public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, String csrfToken) {
		response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("AT", accessToken,
				Duration.ofMillis(jwtService.getExpirationAtMillis()), cookieProps.getAccess()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("RT", refreshToken,
				Duration.ofMillis(jwtService.getExpirationRtMillis()), cookieProps.getRefresh()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("CSRF-TOKEN", csrfToken,
				Duration.ofDays(7), cookieProps.getCsrf()).toString());
	}

	public void clearAuthCookies(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("AT", "", Duration.ZERO, cookieProps.getAccess()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("RT", "", Duration.ZERO, cookieProps.getRefresh()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("CSRF-TOKEN", "", Duration.ZERO, cookieProps.getCsrf()).toString());
	}

	private ResponseCookie buildCookie(String name, String value, Duration maxAge, AuthCookieProperties.CookieOption option) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
				.httpOnly(option.isHttpOnly())
				.secure(option.isSecure())
				.sameSite(option.getSameSite())
				.path(option.getPath());

		if (option.getDomain() != null && !option.getDomain().isBlank()) {
			builder.domain(option.getDomain());
		}

		if (Duration.ZERO.equals(maxAge)) {
			builder.maxAge(0);
		} else {
			builder.maxAge(maxAge);
		}
		return builder.build();
	}
}
