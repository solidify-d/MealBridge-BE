package com.mealbridge.domain.mealsession.dto;

import com.mealbridge.domain.donation.entity.MealType;

import java.math.BigDecimal;
import java.util.List;

public record MealSlotSummary(
		MealType mealType,
		BigDecimal preparedKg,
		BigDecimal wastedKg,
		BigDecimal servedKg,
		BigDecimal wastePercent,
		int sessionCount,
		List<MealItemBreakdown> items,
		List<MealSessionBreakdown> sessions,
		List<MealTrackingEventDetail> events
) {
}
