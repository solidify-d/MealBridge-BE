package com.mealbridge.domain.mealsession.dto;

import com.mealbridge.domain.mealsession.entity.TrackingMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MealTrackingEventDetail(
		LocalDateTime occurredAt,
		String menuName,
		TrackingMode mode,
		BigDecimal totalKg,
		List<LineItemSnapshot> lineItems
) {
}
