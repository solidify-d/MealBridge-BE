package com.mealbridge.common.profileimage;

import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("test")
@RequiredArgsConstructor
public class DbProfileImageUrlCache implements ProfileImageUrlCache {

	private final UserRepository userRepository;

	@Override
	public String getUrl(UUID userId) {
		return userRepository.findById(userId)
				.map(User::getProfileImageUrl)
				.orElse(null);
	}

	@Override
	public void put(UUID userId, String profileImageUrl) {
		// no-op in test profile (no Redis)
	}

	@Override
	public void evict(UUID userId) {
		// no-op in test profile
	}
}
