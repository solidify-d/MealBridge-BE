package com.mealbridge.common.auth.filter;

import com.mealbridge.common.apiResponse.GatewayErrorResponse;
import com.mealbridge.common.auth.GatewayAuthConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

	private static final List<String> AUTH_WHITELIST = List.of(
			"/auth/**",
			"/auth/register/**",
			"/health",
			"/public/**"
	);

	private static final Set<String> SERVICE_PREFIXES = Set.of(
			"/user-service",
			"/donation-service",
			"/matching-service",
			"/ai-service"
	);

	private static final Set<String> STRIP_HEADERS = Set.of(
			"X-User-Id",
			"X-User-Role",
			"X-User-Email",
			GatewayAuthConstants.INTERNAL_SECRET_HEADER
	);

	private final String secretKeyValue;
	private final String gatewayInternalSecret;
	private final AntPathMatcher matcher = new AntPathMatcher();
	private SecretKey atKey;

	public JwtAuthFilter(
			@Value("${jwt.secretKeyAt}") String secretKeyAtValue,
			@Value("${mealbridge.gateway.internal-secret}") String gatewayInternalSecret) {
		this.secretKeyValue = secretKeyAtValue;
		this.gatewayInternalSecret = gatewayInternalSecret;
	}

	@PostConstruct
	public void initKey() {
		this.atKey = decodeKey(secretKeyValue);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();

		if (HttpMethod.OPTIONS.equals(request.getMethod())) {
			return chain.filter(exchange);
		}

		String rawPath = request.getURI().getPath();
		String normalizedPath = normalize(rawPath);

		if (isWhitelisted(normalizedPath)) {
			return chain.filter(stripSpoofedHeaders(exchange));
		}

		HttpCookie atCookie = request.getCookies().getFirst("AT");
		if (atCookie == null || atCookie.getValue() == null || atCookie.getValue().isBlank()) {
			return writeJson(exchange, HttpStatus.UNAUTHORIZED, "Access token is missing. Please login first.");
		}

		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(atKey)
					.build()
					.parseClaimsJws(atCookie.getValue())
					.getBody();

			String userId = Optional.ofNullable(claims.getSubject()).orElse("");
			String role = Optional.ofNullable(claims.get("role", String.class)).orElse("");
			String email = Optional.ofNullable(claims.get("email", String.class)).orElse("");

			ServerHttpRequest.Builder builder = stripSpoofedHeaders(exchange).getRequest().mutate()
					.header("X-User-Id", userId)
					.header("X-User-Role", role)
					.header(GatewayAuthConstants.INTERNAL_SECRET_HEADER, gatewayInternalSecret);
			if (!email.isBlank()) {
				builder.header("X-User-Email", email);
			}

			return chain.filter(exchange.mutate().request(builder.build()).build());
		} catch (JwtException e) {
			log.warn("[JWT] Invalid token for path {}: {}", rawPath, e.getMessage());
			return writeJson(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired access token.");
		}
	}

	private ServerWebExchange stripSpoofedHeaders(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		boolean hasSpoofed = STRIP_HEADERS.stream().anyMatch(h -> request.getHeaders().containsKey(h));
		if (!hasSpoofed) {
			return exchange;
		}
		ServerHttpRequest.Builder builder = request.mutate();
		STRIP_HEADERS.forEach(h -> builder.headers(headers -> headers.remove(h)));
		return exchange.mutate().request(builder.build()).build();
	}

	private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String message) {
		return GatewayErrorResponse.write(exchange, status, message);
	}

	private boolean isWhitelisted(String path) {
		for (String pattern : AUTH_WHITELIST) {
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

	private SecretKey decodeKey(String value) {
		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(value);
		} catch (IllegalArgumentException ex) {
			keyBytes = value.getBytes(StandardCharsets.UTF_8);
		}
		return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
	}
}
