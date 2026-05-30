package com.mealbridge.common.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealbridge.common.apiResponse.CommonDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@ConditionalOnProperty(name = "mealbridge.gateway.internal-auth.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayInternalAuthWebFilter extends OncePerRequestFilter {

	private static final String INTERNAL_SECRET_HEADER = "X-Gateway-Internal-Secret";

	private static final List<String> PUBLIC_PATHS = List.of(
			"/health",
			"/auth/**"
	);

	private final String expectedSecret;
	private final AntPathMatcher matcher = new AntPathMatcher();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public GatewayInternalAuthWebFilter(
			@Value("${mealbridge.gateway.internal-secret}") String expectedSecret) {
		this.expectedSecret = expectedSecret;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		for (String pattern : PUBLIC_PATHS) {
			if (matcher.match(pattern, path)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String provided = request.getHeader(INTERNAL_SECRET_HEADER);
		if (provided == null || !provided.equals(expectedSecret)) {
			writeForbidden(response);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private void writeForbidden(HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		CommonDTO body = CommonDTO.builder()
				.status_message("Direct service access is not allowed")
				.status_code(HttpStatus.FORBIDDEN.value())
				.result(null)
				.build();
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
