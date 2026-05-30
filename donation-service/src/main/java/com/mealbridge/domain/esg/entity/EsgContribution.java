package com.mealbridge.domain.esg.entity;

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
@Table(name = "esg_contributions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EsgContribution extends BaseIdAndTimeEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false, length = 32)
	private EsgActorType actorType;

	@Column(name = "actor_id", nullable = false)
	private UUID actorId;

	@Column(nullable = false, length = 7)
	private String month;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	private EsgEventType eventType;

	@Column(name = "weight_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal weightKg;

	@Column(name = "co2e_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal co2eKg;

	@Column(name = "source_ref")
	private UUID sourceRef;

	@Column(name = "recorded_at", nullable = false)
	private LocalDateTime recordedAt;

	@Builder
	public EsgContribution(
			EsgActorType actorType,
			UUID actorId,
			String month,
			EsgEventType eventType,
			BigDecimal weightKg,
			BigDecimal co2eKg,
			UUID sourceRef,
			LocalDateTime recordedAt) {
		this.actorType = actorType;
		this.actorId = actorId;
		this.month = month;
		this.eventType = eventType;
		this.weightKg = weightKg;
		this.co2eKg = co2eKg;
		this.sourceRef = sourceRef;
		this.recordedAt = recordedAt;
	}
}
