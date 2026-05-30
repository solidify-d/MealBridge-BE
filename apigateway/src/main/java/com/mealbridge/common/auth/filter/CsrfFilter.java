package com.mealbridge.common.auth.filter;

import com.mealbridge.common.apiResponse.GatewayErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
public class CsrfFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);

	private static final List<String> CSRF_WHITELIST = List.of(
			"/auth/**",
			"/health",
			"/public/**"
	);

	private static final Set<String> SERVICE_PREFIXES = Set.of(
			"/user-service",
			"/donation-service",
			"/matching-service",
			"/ai-service"
	);

	private final AntPathMatcher matcher = new AntPathMatcher();

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		HttpMethod method = request.getMethod();

		if (method == null || HttpMethod.OPTIONS.equals(method) || HttpMethod.GET.equals(method)) {
			return chain.filter(exchange);
		}

		String path = normalize(request.getURI().getRawPath());
		if (isWhitelisted(path)) {
			return chain.filter(exchange);
		}

		HttpCookie csrfCookie = request.getCookies().getFirst("CSRF-TOKEN");
		String csrfHeader = request.getHeaders().getFirst("X-CSRF-Token");

		if (csrfCookie == null || csrfCookie.getValue() == null
				|| csrfHeader == null
				|| !csrfCookie.getValue().equals(csrfHeader)) {
			log.warn("[CSRF] Invalid or missing token for {}", request.getURI().getRawPath());
			return GatewayErrorResponse.write(exchange, HttpStatus.FORBIDDEN, "Invalid CSRF token.");
		}

		return chain.filter(exchange);
	}

	private boolean isWhitelisted(String path) {
		for (String pattern : CSRF_WHITELIST) {
			if (matcher.match(pattern, path)) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String rawPath) {
		if (rawPath == null || rawPath.isEmpty()) {
			return "/";
		}
		for (String prefix : SERVICE_PREFIXES) {
			if (rawPath.startsWith(prefix)) {
				String trimmed = rawPath.substring(prefix.length());
				return trimmed.isEmpty() ? "/" : trimmed;
			}
		}
		return rawPath;
	}
}
