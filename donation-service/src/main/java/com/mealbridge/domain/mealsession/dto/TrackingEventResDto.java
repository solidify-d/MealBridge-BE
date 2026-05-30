package com.mealbridge.domain.mealsession.dto;

import com.mealbridge.domain.mealsession.entity.TrackingMode;
import com.mealbridge.domain.mealsession.entity.TrackingSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TrackingEventResDto(
		UUID id,
		TrackingMode mode,
		TrackingSource source,
		BigDecimal deltaKg,
		List<LineItemSnapshot> lineItems,
		LocalDateTime reviewedAt,
		LocalDateTime createdAt
) {
}
