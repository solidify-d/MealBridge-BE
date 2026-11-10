package com.mealbridge.domain.mealsession.dto;

import com.mealbridge.domain.weather.dto.SessionWeatherResDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealSessionBreakdown(
		LocalDate sessionDate,
		String menuName,
		Integer headcount,
		BigDecimal preparedKg,
		BigDecimal servedKg,
		BigDecimal wastedKg,
		List<MealItemBreakdown> items,
		SessionWeatherResDto weather
) {
}
