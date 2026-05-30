package com.mealbridge.domain.matching.entity;

import com.mealbridge.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "organization_pickup_warnings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationPickupWarning extends BaseIdAndTimeEntity {

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "pickup_assignment_id", nullable = false)
	private UUID pickupAssignmentId;

	@Column(name = "donation_id", nullable = false)
	private UUID donationId;

	@Column(nullable = false, length = 500)
	private String reason;

	@Builder
	public OrganizationPickupWarning(
			UUID organizationId,
			UUID pickupAssignmentId,
			UUID donationId,
			String reason) {
		this.organizationId = organizationId;
		this.pickupAssignmentId = pickupAssignmentId;
		this.donationId = donationId;
		this.reason = reason;
	}
}
