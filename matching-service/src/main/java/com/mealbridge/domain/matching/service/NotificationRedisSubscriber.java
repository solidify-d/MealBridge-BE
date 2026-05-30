package com.mealbridge.domain.matching.service;

import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.domain.matching.dto.NotificationSseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ConditionalOnProperty(name = "mealbridge.notifications.redis.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class NotificationRedisSubscriber implements MessageListener {

	private final NotificationSseHub notificationSseHub;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String body = new String(message.getBody(), StandardCharsets.UTF_8);
		try {
			NotificationSseEvent event = JsonUtil.fromJson(body, NotificationSseEvent.class);
			if (event == null || event.recipientUserId() == null || event.notification() == null) {
				return;
			}
			notificationSseHub.deliverLocally(event.recipientUserId(), event.notification());
		} catch (RuntimeException ex) {
			log.warn("Failed to handle Redis notification message: {}", ex.getMessage());
		}
	}
}
