package com.mealbridge.common.auth.filter;

import com.mealbridge.common.apiResponse.GatewayErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthRateLimitFilter implements GlobalFilter, Ordered {

	private static final List<String> RATE_LIMITED = List.of(
			"/auth/login",
			"/auth/register/**"
	);

	private static final Set<String> SERVICE_PREFIXES = Set.of(
			"/user-service",
			"/donation-service",
			"/matching-service",
			"/ai-service"
	);

	private final AntPathMatcher matcher = new AntPathMatcher();
	private final int maxRequestsPerWindow;
	private final long windowMillis;
	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public AuthRateLimitFilter(
			@Value("${mealbridge.security.auth-rate-limit.max-requests:30}") int maxRequestsPerWindow,
			@Value("${mealbridge.security.auth-rate-limit.window-ms:60000}") long windowMillis) {
		this.maxRequestsPerWindow = maxRequestsPerWindow;
		this.windowMillis = windowMillis;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = normalize(exchange.getRequest().getURI().getPath());
		if (!isRateLimited(path)) {
			return chain.filter(exchange);
		}

		String clientKey = resolveClientKey(exchange.getRequest());
		long now = System.currentTimeMillis();
		Window window = windows.compute(clientKey, (key, existing) -> {
			if (existing == null || now - existing.startMillis > windowMillis) {
				return new Window(now, new AtomicInteger(0));
			}
			return existing;
		});

		int count = window.counter.incrementAndGet();
		if (count > maxRequestsPerWindow) {
			return GatewayErrorResponse.write(exchange, HttpStatus.TOO_MANY_REQUESTS,
					"Too many authentication attempts. Please try again later.");
		}

		return chain.filter(exchange);
	}

	private boolean isRateLimited(String path) {
		for (String pattern : RATE_LIMITED) {
			if (matcher.match(pattern, path)) {
				return true;
			}
		}
		return false;
	}

	private String resolveClientKey(ServerHttpRequest request) {
		String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		InetSocketAddress remote = request.getRemoteAddress();
		return remote != null ? remote.getAddress().getHostAddress() : "unknown";
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

	private static final class Window {
		private final long startMillis;
		private final AtomicInteger counter;

		private Window(long startMillis, AtomicInteger counter) {
			this.startMillis = startMillis;
			this.counter = counter;
		}
	}
}
