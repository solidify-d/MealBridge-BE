package com.mealbridge.domain.matching.service;

import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.domain.matching.dto.NotificationResDto;
import com.mealbridge.domain.matching.dto.NotificationSseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "mealbridge.notifications.redis.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisNotificationBroadcaster implements NotificationBroadcaster {

	private final StringRedisTemplate redisTemplate;

	@Value("${mealbridge.notifications.redis.channel:mealbridge:notifications:sse}")
	private String channel;

	@Override
	public void broadcast(UUID recipientUserId, NotificationResDto notification) {
		String payload = JsonUtil.toJson(new NotificationSseEvent(recipientUserId, notification));
		redisTemplate.convertAndSend(channel, payload);
	}
}
