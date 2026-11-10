package com.mealbridge.common.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisEmailVerificationStore implements EmailVerificationStore {

	private static final String KEY_CODE = "email:verify:code:";
	private static final String KEY_VERIFIED = "email:verify:verified:";
	private static final String KEY_SEND_LOCK = "email:verify:send-lock:";

	private final StringRedisTemplate redisTemplate;

	@Override
	public void saveCode(String email, String code, Duration ttl) {
		redisTemplate.opsForValue().set(KEY_CODE + normalize(email), code, ttl);
	}

	@Override
	public Optional<String> getCode(String email) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_CODE + normalize(email)));
	}

	@Override
	public void deleteCode(String email) {
		redisTemplate.delete(KEY_CODE + normalize(email));
	}

	@Override
	public void markVerified(String email, Duration ttl) {
		redisTemplate.opsForValue().set(KEY_VERIFIED + normalize(email), "1", ttl);
	}

	@Override
	public boolean isVerified(String email) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_VERIFIED + normalize(email)));
	}

	@Override
	public void deleteVerified(String email) {
		redisTemplate.delete(KEY_VERIFIED + normalize(email));
	}

	@Override
	public boolean tryAcquireSendLock(String email, Duration cooldown) {
		String key = KEY_SEND_LOCK + normalize(email);
		Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", cooldown);
		return Boolean.TRUE.equals(acquired);
	}

	private String normalize(String email) {
		return email.trim().toLowerCase();
	}
}
