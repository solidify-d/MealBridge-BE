package com.mealbridge.common.profileimage;

import com.mealbridge.common.config.ProfileImageCacheProperties;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisProfileImageUrlCache implements ProfileImageUrlCache {

	static final String KEY_PREFIX = "profileimg:";
	private static final String EMPTY_MARKER = "";

	private final StringRedisTemplate redisTemplate;
	private final UserRepository userRepository;
	private final ProfileImageCacheProperties properties;

	@Override
	public String getUrl(UUID userId) {
		String cached = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
		if (cached != null) {
			return cached.isEmpty() ? null : cached;
		}
		String fromDb = userRepository.findById(userId)
				.map(User::getProfileImageUrl)
				.orElse(null);
		put(userId, fromDb);
		return fromDb;
	}

	@Override
	public void put(UUID userId, String profileImageUrl) {
		String key = KEY_PREFIX + userId;
		Duration ttl = Duration.ofDays(properties.cacheTtlDays());
		if (profileImageUrl == null || profileImageUrl.isBlank()) {
			redisTemplate.opsForValue().set(key, EMPTY_MARKER, ttl);
			return;
		}
		redisTemplate.opsForValue().set(key, profileImageUrl, ttl);
	}

	@Override
	public void evict(UUID userId) {
		redisTemplate.delete(KEY_PREFIX + userId);
	}
}
