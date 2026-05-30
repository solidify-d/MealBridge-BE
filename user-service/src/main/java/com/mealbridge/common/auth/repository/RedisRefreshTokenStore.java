package com.mealbridge.common.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String KEY_RT = "rt:";
	private static final String KEY_BLACKLIST = "rt:blacklist:";

	private final StringRedisTemplate redisTemplate;

	@Override
	public void saveRefreshToken(UUID userId, String rt, long ttlMillis) {
		redisTemplate.opsForValue().set(KEY_RT + userId, rt, Duration.ofMillis(ttlMillis));
	}

	@Override
	public String getRefreshToken(UUID userId) {
		return redisTemplate.opsForValue().get(KEY_RT + userId);
	}

	@Override
	public void revoke(String jti, long ttlMillis) {
		if (jti == null) {
			return;
		}
		redisTemplate.opsForValue().set(KEY_BLACKLIST + jti, "1", Duration.ofMillis(Math.max(ttlMillis, 1)));
	}

	@Override
	public boolean isRevoked(String jti) {
		return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_BLACKLIST + jti));
	}

	@Override
	public void deleteRefreshToken(UUID userId) {
		redisTemplate.delete(KEY_RT + userId);
	}
}
