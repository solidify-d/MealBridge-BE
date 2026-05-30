package com.mealbridge.domain.procurement.dto;

import com.mealbridge.domain.weather.dto.CurrentWeatherResDto;

import java.time.LocalDate;
import java.util.List;

public record ProcurementSuggestionResDto(
		LocalDate from,
		LocalDate to,
		ProcurementDataContextDto dataContext,
		String methodologyDisclaimer,
		List<MealProcurementSuggestionDto> suggestions) {

	public record ProcurementDataContextDto(
			int historicalSessionCount,
			java.math.BigDecimal avgWasteRatePct,
			java.math.BigDecimal avgServedKgPerPerson,
			java.math.BigDecimal recentWasteTrendPct,
			CurrentWeatherResDto currentWeather,
			String institutionName) {
	}
}
