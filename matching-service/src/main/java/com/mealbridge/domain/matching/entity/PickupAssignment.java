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
@Table(name = "pickup_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickupAssignment extends BaseIdAndTimeEntity {

	@Column(name = "donation_id", nullable = false)
	private UUID donationId;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private PickupAssignmentStatus status;

	@Column(nullable = false)
	private double score;

	@Column(name = "route_plan_id")
	private UUID routePlanId;

	@Column(name = "estimated_arrival_at")
	private java.time.LocalDateTime estimatedArrivalAt;

	@Column(name = "in_transit_at")
	private java.time.LocalDateTime inTransitAt;

	@Column(name = "completed_at")
	private java.time.LocalDateTime completedAt;

	@Column(name = "cancelled_at")
	private java.time.LocalDateTime cancelledAt;

	@Builder
	public PickupAssignment(UUID donationId, UUID organizationId, PickupAssignmentStatus status, double score) {
		this.donationId = donationId;
		this.organizationId = organizationId;
		this.status = status;
		this.score = score;
	}

	public void accept() {
		this.status = PickupAssignmentStatus.ACCEPTED;
	}

	public void linkRoutePlan(UUID routePlanId, java.time.LocalDateTime estimatedArrivalAt) {
		this.routePlanId = routePlanId;
		this.estimatedArrivalAt = estimatedArrivalAt;
	}

	public void markInTransit(java.time.LocalDateTime at) {
		this.status = PickupAssignmentStatus.IN_TRANSIT;
		this.inTransitAt = at;
	}

	public void complete(java.time.LocalDateTime at) {
		this.status = PickupAssignmentStatus.COMPLETED;
		this.completedAt = at;
	}

	public void cancel(java.time.LocalDateTime at) {
		this.status = PickupAssignmentStatus.CANCELLED;
		this.cancelledAt = at;
	}

	public void updateEstimatedArrival(java.time.LocalDateTime estimatedArrivalAt) {
		this.estimatedArrivalAt = estimatedArrivalAt;
	}
}
