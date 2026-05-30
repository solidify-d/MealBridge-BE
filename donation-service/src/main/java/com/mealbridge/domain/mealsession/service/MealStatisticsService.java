package com.mealbridge.domain.mealsession.service;

import com.mealbridge.domain.mealsession.dto.MealStatisticsResDto;

import java.time.LocalDate;
import java.util.UUID;

public interface MealStatisticsService {

	MealStatisticsResDto mealStatistics(
			UUID actorUserId,
			UUID donorInstitutionId,
			LocalDate from,
			LocalDate to);
}
