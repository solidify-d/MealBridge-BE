package com.mealbridge.domain.matching.service;

import com.mealbridge.domain.matching.dto.NotificationResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "mealbridge.notifications.redis.enabled", havingValue = "false")
@RequiredArgsConstructor
public class LocalNotificationBroadcaster implements NotificationBroadcaster {

	private final NotificationSseHub notificationSseHub;

	@Override
	public void broadcast(UUID recipientUserId, NotificationResDto notification) {
		notificationSseHub.deliverLocally(recipientUserId, notification);
	}
}
