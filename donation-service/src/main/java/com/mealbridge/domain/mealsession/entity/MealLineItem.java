package com.mealbridge.domain.mealsession.entity;

import com.mealbridge.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "meal_line_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealLineItem extends BaseIdAndTimeEntity {

	@Column(name = "session_id", nullable = false)
	private UUID sessionId;

	@Column(name = "item_name", nullable = false, length = 200)
	private String itemName;

	@Column(name = "prepared_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal preparedKg;

	@Column(name = "remaining_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal remainingKg;

	@Column(length = 500)
	private String ingredients;

	@Column(name = "per_person_kg", precision = 12, scale = 3)
	private BigDecimal perPersonKg;

	@Builder
	public MealLineItem(
			UUID sessionId,
			String itemName,
			BigDecimal preparedKg,
			BigDecimal perPersonKg,
			String ingredients) {
		this.sessionId = sessionId;
		this.itemName = itemName.trim();
		this.preparedKg = preparedKg;
		this.remainingKg = preparedKg;
		this.perPersonKg = perPersonKg;
		this.ingredients = ingredients;
	}

	public void setRemainingKg(BigDecimal remainingKg) {
		if (remainingKg == null || remainingKg.signum() < 0) {
			throw new IllegalArgumentException("Remaining kg must be non-negative");
		}
		this.remainingKg = remainingKg;
	}
}
