package com.mealbridge.domain.matching.entity;

import com.mealbridge.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseIdAndTimeEntity {

	@Column(name = "recipient_user_id", nullable = false)
	private UUID recipientUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 1000)
	private String body;

	@Column(name = "payload_json", columnDefinition = "TEXT")
	private String payloadJson;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Builder
	public Notification(
			UUID recipientUserId,
			NotificationType type,
			String title,
			String body,
			String payloadJson) {
		this.recipientUserId = recipientUserId;
		this.type = type;
		this.title = title;
		this.body = body;
		this.payloadJson = payloadJson;
	}

	public void markRead(LocalDateTime readAt) {
		this.readAt = readAt;
	}
}
