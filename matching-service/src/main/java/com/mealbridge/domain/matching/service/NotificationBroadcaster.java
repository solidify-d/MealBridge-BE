package com.mealbridge.domain.matching.service;

import com.mealbridge.domain.matching.dto.NotificationResDto;

import java.util.UUID;

public interface NotificationBroadcaster {

	void broadcast(UUID recipientUserId, NotificationResDto notification);
}
