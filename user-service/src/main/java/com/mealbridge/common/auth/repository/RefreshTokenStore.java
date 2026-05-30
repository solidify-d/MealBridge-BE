package com.mealbridge.common.auth.repository;

import java.util.UUID;

public interface RefreshTokenStore {

	void saveRefreshToken(UUID userId, String rt, long ttlMillis);

	String getRefreshToken(UUID userId);

	void revoke(String jti, long ttlMillis);

	boolean isRevoked(String jti);

	void deleteRefreshToken(UUID userId);
}
