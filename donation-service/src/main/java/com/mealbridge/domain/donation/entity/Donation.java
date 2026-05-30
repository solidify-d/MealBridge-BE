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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "donations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE donations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Donation extends BaseIdAndTimeEntity {

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

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

	@Column(name = "before_image_url", length = 1024)
	private String beforeImageUrl;

	@Column(name = "after_image_url", length = 1024)
	private String afterImageUrl;

	@Column(length = 1000)
	private String notes;

	@Builder
	public Donation(
			UUID organizationId,
			UUID registeredByUserId,
			MealType mealType,
			String menuName,
			LocalDateTime servingEndedAt,
			LocalDateTime storageStartedAt,
			String beforeImageUrl,
			String afterImageUrl,
			String notes) {
		this.organizationId = organizationId;
		this.registeredByUserId = registeredByUserId;
		this.status = DonationStatus.REGISTERED;
		this.mealType = mealType;
		this.menuName = menuName;
		this.servingEndedAt = servingEndedAt;
		this.storageStartedAt = storageStartedAt;
		this.beforeImageUrl = beforeImageUrl;
		this.afterImageUrl = afterImageUrl;
		this.notes = notes;
	}

	public void cancel() {
		if (this.status != DonationStatus.REGISTERED) {
			throw new IllegalStateException("Only registered donations can be cancelled");
		}
		this.status = DonationStatus.CANCELLED;
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
