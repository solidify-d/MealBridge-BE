package com.mealbridge.domain.procurement.dto;

import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.prediction.dto.PredictedLeftoverResDto.SimilarSessionRef;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MealProcurementSuggestionDto(
		UUID mealPlanId,
		LocalDate planDate,
		MealType mealType,
		Integer headcount,
		BigDecimal currentPreparedKg,
		BigDecimal suggestedPreparedKg,
		BigDecimal deltaKg,
		BigDecimal deltaPercent,
		BigDecimal predictedWasteKg,
		BigDecimal expectedServedKg,
		BigDecimal confidenceScore,
		Integer referenceSessionCount,
		WeatherContextDto weatherContext,
		List<String> factorsUsed,
		List<LineItemProcurementSuggestionDto> lineItems,
		List<SimilarSessionRef> similarSessions,
		String rationale) {

	public record WeatherContextDto(
			Integer weatherCode,
			String weatherLabel,
			Double temperatureC,
			Integer humidityPct,
			String source) {
	}

	public record LineItemProcurementSuggestionDto(
			String itemName,
			BigDecimal currentPreparedKg,
			BigDecimal suggestedPreparedKg,
			BigDecimal deltaKg) {
	}
}
