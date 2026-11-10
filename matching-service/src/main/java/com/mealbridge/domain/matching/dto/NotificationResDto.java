package com.mealbridge.domain.matching.dto;

import com.mealbridge.domain.matching.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationResDto(
		UUID id,
		NotificationType type,
		String title,
		String body,
		Map<String, Object> payload,
		LocalDateTime readAt,
		LocalDateTime createdAt) {
}
