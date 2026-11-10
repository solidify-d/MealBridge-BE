package com.mealbridge.domain.mealplan.service;

import com.mealbridge.domain.mealplan.dto.BulkMealPlanReq;
import com.mealbridge.domain.mealplan.dto.MealPlanResDto;
import com.mealbridge.domain.mealplan.dto.UpsertMealPlanReq;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MealPlanService {

	List<MealPlanResDto> list(UUID actorUserId, UUID donorInstitutionId, LocalDate from, LocalDate to);

	MealPlanResDto upsert(UUID actorUserId, UpsertMealPlanReq request);

	List<MealPlanResDto> bulkUpsert(UUID actorUserId, BulkMealPlanReq request);

	MealPlanResDto getForDate(UUID actorUserId, UUID donorInstitutionId, LocalDate planDate, com.mealbridge.domain.donation.entity.MealType mealType);

	void delete(UUID actorUserId, UUID donorInstitutionId, UUID planId);
}
