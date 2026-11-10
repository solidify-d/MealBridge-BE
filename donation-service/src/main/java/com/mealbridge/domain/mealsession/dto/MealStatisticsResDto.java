package com.mealbridge.domain.mealsession.dto;

import java.time.LocalDate;
import java.util.List;

public record MealStatisticsResDto(
		LocalDate from,
		LocalDate to,
		boolean singleDay,
		List<MealTypeDailyPoint> servingDaily,
		List<MealTypeDailyPoint> leftoverDaily,
		List<MealTypeHourlyPoint> servingHourly,
		List<MealTypeHourlyPoint> leftoverHourly,
		List<MealSlotSummary> mealSummaries
) {
}
