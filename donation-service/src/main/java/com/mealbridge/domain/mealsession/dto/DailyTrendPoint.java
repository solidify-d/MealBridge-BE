package com.mealbridge.domain.mealsession.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTrendPoint(
		LocalDate date,
		BigDecimal totalKg,
		int eventCount
) {
}
