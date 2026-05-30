package com.mealbridge.domain.mealsession.entity;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tracking_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackingEvent extends BaseIdAndTimeEntity {

	@Column(name = "session_id", nullable = false)
	private UUID sessionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TrackingMode mode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TrackingSource source;

	@Column(name = "delta_kg", precision = 12, scale = 3)
	private BigDecimal deltaKg;

	@Column(name = "line_item_snapshots", columnDefinition = "TEXT")
	private String lineItemSnapshots;

	@Column(name = "ai_raw_response", columnDefinition = "TEXT")
	private String aiRawResponse;

	@Column(nullable = false)
	private boolean reviewed;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@Builder
	public TrackingEvent(
			UUID sessionId,
			TrackingMode mode,
			TrackingSource source,
			BigDecimal deltaKg,
			String lineItemSnapshots,
			String aiRawResponse) {
		this.sessionId = sessionId;
		this.mode = mode;
		this.source = source;
		this.deltaKg = deltaKg;
		this.lineItemSnapshots = lineItemSnapshots;
		this.aiRawResponse = aiRawResponse;
		this.reviewed = true;
		this.reviewedAt = LocalDateTime.now();
	}
}
