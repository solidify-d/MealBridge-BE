package com.mealbridge.domain.matching.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.matching.dto.NotificationResDto;
import com.mealbridge.domain.matching.entity.Notification;
import com.mealbridge.domain.matching.entity.NotificationType;
import com.mealbridge.domain.matching.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationBroadcaster notificationBroadcaster;

	@Transactional
	public NotificationResDto create(
			UUID recipientUserId,
			NotificationType type,
			String title,
			String body,
			Map<String, Object> payload) {
		String payloadJson = payload != null ? JsonUtil.toJson(payload) : null;
		Notification notification = Notification.builder()
				.recipientUserId(recipientUserId)
				.type(type)
				.title(title)
				.body(body)
				.payloadJson(payloadJson)
				.build();
		notificationRepository.save(notification);
		NotificationResDto dto = toDto(notification);
		notificationBroadcaster.broadcast(recipientUserId, dto);
		return dto;
	}

	@Transactional(readOnly = true)
	public List<NotificationResDto> list(UUID userId, int page, int size) {
		Page<Notification> notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(
				userId, PageRequest.of(page, size));
		return notifications.stream().map(this::toDto).toList();
	}

	@Transactional(readOnly = true)
	public long unreadCount(UUID userId) {
		return notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId);
	}

	@Transactional
	public NotificationResDto markRead(UUID userId, UUID notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new EntityNotFoundException("Notification not found"));
		if (!notification.getRecipientUserId().equals(userId)) {
			throw new SecurityException("Access denied");
		}
		if (notification.getReadAt() == null) {
			notification.markRead(KstDates.now());
		}
		return toDto(notification);
	}

	@Transactional
	public void markAllRead(UUID userId) {
		notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 200))
				.forEach(notification -> {
					if (notification.getReadAt() == null) {
						notification.markRead(KstDates.now());
					}
				});
	}

	private NotificationResDto toDto(Notification notification) {
		Map<String, Object> payload = notification.getPayloadJson() != null
				? JsonUtil.fromJson(notification.getPayloadJson(), new TypeReference<Map<String, Object>>() {
				})
				: Map.of();
		return new NotificationResDto(
				notification.getId(),
				notification.getType(),
				notification.getTitle(),
				notification.getBody(),
				payload,
				notification.getReadAt(),
				notification.getCreatedAt());
	}
}
