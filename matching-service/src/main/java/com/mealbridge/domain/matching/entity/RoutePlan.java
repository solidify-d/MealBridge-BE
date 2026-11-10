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

import java.util.UUID;

@Entity
@Table(name = "route_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutePlan extends BaseIdAndTimeEntity {

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "planned_stops_json", columnDefinition = "TEXT", nullable = false)
	private String plannedStopsJson;

	@Column(name = "total_distance_m", nullable = false)
	private long totalDistanceM;

	@Column(name = "total_duration_s", nullable = false)
	private long totalDurationS;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private RoutePlanStatus status;

	@Builder
	public RoutePlan(
			UUID organizationId,
			String plannedStopsJson,
			long totalDistanceM,
			long totalDurationS,
			RoutePlanStatus status) {
		this.organizationId = organizationId;
		this.plannedStopsJson = plannedStopsJson;
		this.totalDistanceM = totalDistanceM;
		this.totalDurationS = totalDurationS;
		this.status = status;
	}

	public void complete() {
		this.status = RoutePlanStatus.COMPLETED;
	}
}
