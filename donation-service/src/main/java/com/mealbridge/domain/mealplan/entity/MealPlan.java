package com.mealbridge.domain.mealplan.entity;

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
@Table(name = "meal_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlan extends BaseIdAndTimeEntity {

	@Column(name = "donor_institution_id", nullable = false)
	private UUID donorInstitutionId;

	@Column(name = "registered_by_user_id", nullable = false)
	private UUID registeredByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "meal_type", nullable = false, length = 32)
	private MealType mealType;

	@Column(name = "plan_date", nullable = false)
	private LocalDate planDate;

	@Column(name = "prepared_weight_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal preparedWeightKg;

	@Column(name = "headcount")
	private Integer headcount;

	@Column(name = "line_items_json", nullable = false, columnDefinition = "TEXT")
	private String lineItemsJson;

	@Column(length = 1000)
	private String notes;

	@Builder
	public MealPlan(
			UUID donorInstitutionId,
			UUID registeredByUserId,
			MealType mealType,
			LocalDate planDate,
			BigDecimal preparedWeightKg,
			Integer headcount,
			String lineItemsJson,
			String notes) {
		this.donorInstitutionId = donorInstitutionId;
		this.registeredByUserId = registeredByUserId;
		this.mealType = mealType;
		this.planDate = planDate;
		this.preparedWeightKg = preparedWeightKg;
		this.headcount = headcount;
		this.lineItemsJson = lineItemsJson;
		this.notes = notes;
	}

	public void updateContents(BigDecimal preparedWeightKg, Integer headcount, String lineItemsJson, String notes) {
		this.preparedWeightKg = preparedWeightKg;
		this.headcount = headcount;
		this.lineItemsJson = lineItemsJson;
		this.notes = notes;
	}
}
