package com.mealbridge.common.auth.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

	private final Map<UUID, String> refreshTokens = new ConcurrentHashMap<>();
	private final Map<String, Boolean> blacklist = new ConcurrentHashMap<>();

	@Override
	public void saveRefreshToken(UUID userId, String rt, long ttlMillis) {
		refreshTokens.put(userId, rt);
	}

	@Override
	public String getRefreshToken(UUID userId) {
		return refreshTokens.get(userId);
	}

	@Override
	public void revoke(String jti, long ttlMillis) {
		if (jti != null) {
			blacklist.put(jti, true);
		}
	}

	@Override
	public boolean isRevoked(String jti) {
		return jti != null && blacklist.containsKey(jti);
	}

	@Override
	public void deleteRefreshToken(UUID userId) {
		refreshTokens.remove(userId);
	}
}
