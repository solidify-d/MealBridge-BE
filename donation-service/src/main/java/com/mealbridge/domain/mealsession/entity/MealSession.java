package com.mealbridge.domain.mealsession.entity;

import com.mealbridge.common.domain.BaseIdAndTimeEntity;
import com.mealbridge.domain.donation.entity.MealType;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "meal_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealSession extends BaseIdAndTimeEntity {

	@Column(name = "donor_institution_id", nullable = false)
	private UUID donorInstitutionId;

	@Column(name = "registered_by_user_id", nullable = false)
	private UUID registeredByUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private MealSessionStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "meal_type", nullable = false, length = 32)
	private MealType mealType;

	@Column(name = "menu_name", nullable = false, length = 200)
	private String menuName;

	@Column(name = "session_date", nullable = false)
	private LocalDate sessionDate;

	@Column(name = "prepared_weight_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal preparedWeightKg;

	@Column(name = "remaining_weight_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal remainingWeightKg;

	@Column(name = "headcount")
	private Integer headcount;

	@Column(length = 1000)
	private String notes;

	@Builder
	public MealSession(
			UUID donorInstitutionId,
			UUID registeredByUserId,
			MealType mealType,
			String menuName,
			LocalDate sessionDate,
			BigDecimal preparedWeightKg,
			Integer headcount,
			String notes) {
		this.donorInstitutionId = donorInstitutionId;
		this.registeredByUserId = registeredByUserId;
		this.status = MealSessionStatus.DRAFT;
		this.mealType = mealType;
		this.menuName = menuName.trim();
		this.sessionDate = sessionDate;
		this.preparedWeightKg = preparedWeightKg;
		this.remainingWeightKg = preparedWeightKg;
		this.headcount = headcount;
		this.notes = notes;
	}

	public void transitionTo(MealSessionStatus next) {
		this.status = next;
	}

	public void applyServingDelta(BigDecimal servedKg) {
		if (servedKg == null || servedKg.signum() < 0) {
			throw new IllegalArgumentException("Served amount must be non-negative");
		}
		this.remainingWeightKg = this.remainingWeightKg.subtract(servedKg).max(BigDecimal.ZERO);
	}

	public void setRemainingWeight(BigDecimal remainingKg) {
		if (remainingKg == null || remainingKg.signum() < 0) {
			throw new IllegalArgumentException("Remaining weight must be non-negative");
		}
		this.remainingWeightKg = remainingKg;
	}

	public void close() {
		this.status = MealSessionStatus.CLOSED;
	}
}
