package com.mealbridge.common.profileimage;

import java.util.UUID;

public interface ProfileImageUrlCache {

	/** Redis {@code profileimg:{userId}} first, then DB. */
	String getUrl(UUID userId);

	void put(UUID userId, String profileImageUrl);

	void evict(UUID userId);
}
