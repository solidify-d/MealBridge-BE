package com.mealbridge.common.security;

import com.mealbridge.domain.user.User;
import com.mealbridge.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String uri = request.getRequestURI();
		if (isWhitelisted(uri)) {
			filterChain.doFilter(request, response);
			return;
		}

		String userHeader = request.getHeader("X-User-Id");
		String roleHeader = request.getHeader("X-User-Role");

		if (userHeader == null || roleHeader == null) {
			log.debug("Missing gateway auth headers for uri={}", uri);
			filterChain.doFilter(request, response);
			return;
		}

		try {
			UUID userId = UUID.fromString(userHeader);
			Optional<User> userOpt = userRepository.findById(userId);
			if (userOpt.isEmpty()) {
				SecurityContextHolder.clearContext();
				filterChain.doFilter(request, response);
				return;
			}

			User user = userOpt.get();
			CustomUserPrincipal principal = new CustomUserPrincipal(user.getId(), user.getRole(), user.getStatus());
			var authentication = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (IllegalArgumentException ex) {
			log.warn("Invalid X-User-Id header: {}", userHeader);
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private boolean isWhitelisted(String uri) {
		return PublicApiPathMatcher.isPublicPath(uri);
	}
}
