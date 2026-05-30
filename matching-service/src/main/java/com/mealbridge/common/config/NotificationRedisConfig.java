package com.mealbridge.common.config;

import com.mealbridge.domain.matching.service.NotificationRedisSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(name = "mealbridge.notifications.redis.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRedisConfig {

	@Bean
	RedisMessageListenerContainer notificationRedisListenerContainer(
			RedisConnectionFactory connectionFactory,
			NotificationRedisSubscriber subscriber,
			@Value("${mealbridge.notifications.redis.channel:mealbridge:notifications:sse}") String channel) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(subscriber, new ChannelTopic(channel));
		return container;
	}
}
