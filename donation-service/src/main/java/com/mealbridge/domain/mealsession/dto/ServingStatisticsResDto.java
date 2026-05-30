package com.mealbridge.domain.mealsession.dto;

import com.mealbridge.domain.donation.entity.MealType;

import java.time.LocalDate;
import java.util.List;

public record ServingStatisticsResDto(
		LocalDate from,
		LocalDate to,
		MealType mealTypeFilter,
		List<DailyTrendPoint> dailyTrend,
		List<HourlyTrendPoint> hourlyTrend,
		List<TrackingEventResDto> events
) {
}
