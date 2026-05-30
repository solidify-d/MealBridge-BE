package com.mealbridge.common.auth.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.dto.DonorRegisterRequest;
import com.mealbridge.common.auth.dto.LoginRequest;
import com.mealbridge.common.auth.dto.LoginResponse;
import com.mealbridge.common.auth.dto.OrganizationRegisterRequest;
import com.mealbridge.common.auth.dto.SuspendedAccountInfo;
import com.mealbridge.common.auth.repository.RefreshTokenStore;
import com.mealbridge.common.auth.service.AuthCookieService;
import com.mealbridge.common.auth.service.JwtService;
import com.mealbridge.common.auth.service.RegistrationService;
import com.mealbridge.common.config.SupportProperties;
import com.mealbridge.common.exception.AccountSuspendedException;
import com.mealbridge.common.exception.UnauthorizedException;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenStore refreshTokenStore;
	private final AuthCookieService authCookieService;
	private final RegistrationService registrationService;
	private final SupportProperties supportProperties;

	@PostMapping("/register/organization")
	public ResponseEntity<?> registerOrganization(@Valid @RequestBody OrganizationRegisterRequest request) {
		var body = registrationService.registerOrganization(request);
		return ApiResponse.created(body, body.message());
	}

	@PostMapping("/register/donor")
	public ResponseEntity<?> registerDonor(@Valid @RequestBody DonorRegisterRequest request) {
		var body = registrationService.registerDonor(request);
		return ApiResponse.created(body, body.message());
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new UnauthorizedException("Invalid email or password");
		}

		if (user.getStatus() == UserStatus.SUSPENDED) {
			String reason = user.getSuspendReason() != null && !user.getSuspendReason().isBlank()
					? user.getSuspendReason()
					: "Your account has been suspended. Please contact support for assistance.";
			String contactEmail = supportProperties.contactEmail();
			if (contactEmail == null || contactEmail.isBlank()) {
				contactEmail = "support@mealbridge.local";
			}
			throw new AccountSuspendedException(new SuspendedAccountInfo(reason, contactEmail));
		}

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UnauthorizedException("Account is not active");
		}

		String role = user.getRole().name();
		String jti = jwtService.newJti();
		String accessToken = jwtService.createAccessToken(user.getId(), role, user.getEmail());
		String refreshToken = jwtService.createRefreshToken(user.getId(), jti);
		String csrfToken = UUID.randomUUID().toString();

		refreshTokenStore.saveRefreshToken(user.getId(), refreshToken, jwtService.getExpirationRtMillis());
		authCookieService.setAuthCookies(response, accessToken, refreshToken, csrfToken);

		LoginResponse body = new LoginResponse(
				user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getStatus());
		return ApiResponse.ok(body, "Login successful");
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@CookieValue(value = "RT", required = false) String rt, HttpServletResponse response) {
		if (rt == null) {
			throw new UnauthorizedException("Refresh token missing");
		}

		Claims rtClaims;
		try {
			rtClaims = jwtService.parseRefreshToken(rt);
		} catch (JwtException e) {
			throw new UnauthorizedException("Invalid refresh token");
		}

		UUID userId = UUID.fromString(rtClaims.getSubject());
		String oldJti = rtClaims.getId();

		if (refreshTokenStore.isRevoked(oldJti)) {
			throw new UnauthorizedException("Refresh token already used");
		}

		String savedRt = refreshTokenStore.getRefreshToken(userId);
		if (savedRt == null || !savedRt.equals(rt)) {
			throw new UnauthorizedException("Refresh token mismatch");
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UnauthorizedException("Account is not active");
		}

		String role = user.getRole().name();
		String newAt = jwtService.createAccessToken(user.getId(), role, user.getEmail());
		String newJti = jwtService.newJti();
		String newRt = jwtService.createRefreshToken(user.getId(), newJti);
		String newCsrf = UUID.randomUUID().toString();

		long ttlMillis = Math.max(0, rtClaims.getExpiration().getTime() - System.currentTimeMillis());
		refreshTokenStore.revoke(oldJti, ttlMillis);
		refreshTokenStore.saveRefreshToken(userId, newRt, jwtService.getExpirationRtMillis());
		authCookieService.setAuthCookies(response, newAt, newRt, newCsrf);

		return ApiResponse.ok(null, "Token refreshed");
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(@CookieValue(value = "RT", required = false) String rt, HttpServletResponse response) {
		if (rt != null) {
			try {
				Claims claims = jwtService.parseRefreshToken(rt);
				UUID userId = UUID.fromString(claims.getSubject());
				String jti = claims.getId();
				long ttlMillis = Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
				refreshTokenStore.revoke(jti, ttlMillis);
				refreshTokenStore.deleteRefreshToken(userId);
			} catch (JwtException ignored) {
				// ignore invalid RT on logout
			}
		}
		authCookieService.clearAuthCookies(response);
		return ApiResponse.ok(null, "Logout successful");
	}
}
