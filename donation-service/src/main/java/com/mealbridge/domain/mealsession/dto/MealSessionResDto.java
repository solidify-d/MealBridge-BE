package com.mealbridge.domain.mealsession.dto;

import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealsession.entity.MealSessionStatus;
import com.mealbridge.domain.prediction.dto.PredictedLeftoverResDto;
import com.mealbridge.domain.weather.dto.SessionWeatherResDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MealSessionResDto(
		UUID id,
		UUID donorInstitutionId,
		MealSessionStatus status,
		MealType mealType,
		String menuName,
		LocalDate sessionDate,
		BigDecimal preparedWeightKg,
		BigDecimal remainingWeightKg,
		Integer headcount,
		String notes,
		List<MealLineItemResDto> lineItems,
		List<TrackingEventResDto> recentEvents,
		SessionWeatherResDto weather,
		PredictedLeftoverResDto predictedLeftover,
		LocalDateTime expectedServingEndAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
