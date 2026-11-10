package com.mealbridge.domain.donation.entity;

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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "donations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE donations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Donation extends BaseIdAndTimeEntity {

	@Column(name = "donor_institution_id", nullable = false)
	private UUID donorInstitutionId;

	@Column(name = "registered_by_user_id", nullable = false)
	private UUID registeredByUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DonationStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "meal_type", nullable = false, length = 32)
	private MealType mealType;

	@Column(name = "menu_name", nullable = false, length = 200)
	private String menuName;

	@Column(name = "serving_ended_at")
	private LocalDateTime servingEndedAt;

	@Column(name = "storage_started_at")
	private LocalDateTime storageStartedAt;

	@Column(name = "pickup_deadline_at")
	private LocalDateTime pickupDeadlineAt;

	@Column(name = "before_image_url", length = 1024)
	private String beforeImageUrl;

	@Column(name = "after_image_url", length = 1024)
	private String afterImageUrl;

	@Column(length = 1000)
	private String notes;

	@Column(name = "meal_session_id")
	private UUID mealSessionId;

	@Column(name = "total_weight_kg", precision = 12, scale = 3)
	private BigDecimal totalWeightKg;

	@Column(name = "line_items_json", columnDefinition = "TEXT")
	private String lineItemsJson;

	@Column(name = "accepted_organization_id")
	private UUID acceptedOrganizationId;

	@Column(name = "accepted_at")
	private LocalDateTime acceptedAt;

	@Column(name = "in_transit_at")
	private LocalDateTime inTransitAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Builder
	public Donation(
			UUID donorInstitutionId,
			UUID registeredByUserId,
			MealType mealType,
			String menuName,
			LocalDateTime servingEndedAt,
			LocalDateTime storageStartedAt,
			LocalDateTime pickupDeadlineAt,
			String beforeImageUrl,
			String afterImageUrl,
			String notes,
			UUID mealSessionId,
			BigDecimal totalWeightKg,
			String lineItemsJson) {
		this.donorInstitutionId = donorInstitutionId;
		this.registeredByUserId = registeredByUserId;
		this.status = DonationStatus.REGISTERED;
		this.mealType = mealType;
		this.menuName = menuName;
		this.servingEndedAt = servingEndedAt;
		this.storageStartedAt = storageStartedAt;
		this.pickupDeadlineAt = pickupDeadlineAt;
		this.beforeImageUrl = beforeImageUrl;
		this.afterImageUrl = afterImageUrl;
		this.notes = notes;
		this.mealSessionId = mealSessionId;
		this.totalWeightKg = totalWeightKg;
		this.lineItemsJson = lineItemsJson;
	}

	public void cancel() {
		if (this.status != DonationStatus.REGISTERED) {
			throw new IllegalStateException("Only registered donations can be cancelled");
		}
		this.status = DonationStatus.CANCELLED;
	}

	public void accept(UUID organizationId, LocalDateTime acceptedAt) {
		if (this.status != DonationStatus.REGISTERED) {
			throw new IllegalStateException("Only registered donations can be accepted");
		}
		if (organizationId == null) {
			throw new IllegalArgumentException("organizationId is required");
		}
		if (!isPickupAvailable(com.mealbridge.common.util.KstDates.now())) {
			throw new IllegalStateException("Donation is no longer available for pickup");
		}
		this.status = DonationStatus.ACCEPTED;
		this.acceptedOrganizationId = organizationId;
		this.acceptedAt = acceptedAt;
	}

	public void markInTransit(LocalDateTime at) {
		if (this.status != DonationStatus.ACCEPTED) {
			throw new IllegalStateException("Only accepted donations can be marked in transit");
		}
		this.status = DonationStatus.IN_TRANSIT;
		this.inTransitAt = at;
	}

	public void complete(LocalDateTime at) {
		if (this.status != DonationStatus.IN_TRANSIT) {
			throw new IllegalStateException("Only in-transit donations can be completed");
		}
		this.status = DonationStatus.COMPLETED;
		this.completedAt = at;
	}

	public void releaseFromTransit() {
		if (this.status != DonationStatus.IN_TRANSIT) {
			throw new IllegalStateException("Only in-transit donations can be released");
		}
		this.status = DonationStatus.REGISTERED;
		this.acceptedOrganizationId = null;
		this.acceptedAt = null;
		this.inTransitAt = null;
	}

	public void expire() {
		if (this.status != DonationStatus.REGISTERED) {
			return;
		}
		this.status = DonationStatus.EXPIRED;
	}

	public boolean isPickupAvailable(LocalDateTime now) {
		return this.status == DonationStatus.REGISTERED
				&& (this.pickupDeadlineAt == null || !this.pickupDeadlineAt.isBefore(now));
	}

	public void updateImages(String beforeImageUrl, String afterImageUrl) {
		if (beforeImageUrl != null) {
			this.beforeImageUrl = beforeImageUrl;
		}
		if (afterImageUrl != null) {
			this.afterImageUrl = afterImageUrl;
		}
	}

	public void clearImages() {
		this.beforeImageUrl = null;
		this.afterImageUrl = null;
	}
}
