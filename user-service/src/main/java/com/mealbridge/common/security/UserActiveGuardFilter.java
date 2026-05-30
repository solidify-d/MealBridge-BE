package com.mealbridge.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealbridge.common.api.ApiResponseBody;
import com.mealbridge.domain.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserActiveGuardFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
			if (principal.status() != UserStatus.ACTIVE) {
				response.setStatus(HttpStatus.FORBIDDEN.value());
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				var body = new ApiResponseBody<>(403, "Account is not active", null);
				objectMapper.writeValue(response.getOutputStream(), body);
				return;
			}
		}
		filterChain.doFilter(request, response);
	}
}
