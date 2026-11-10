package com.mealbridge.domain.mealsession.service;

import com.mealbridge.domain.mealsession.dto.ConfirmTrackingEventReq;
import com.mealbridge.domain.mealsession.dto.CreateMealSessionReq;
import com.mealbridge.domain.mealsession.dto.MealSessionResDto;

import java.util.UUID;

public interface MealSessionService {

	MealSessionResDto create(UUID actorUserId, CreateMealSessionReq request);

	MealSessionResDto getActive(UUID actorUserId, UUID donorInstitutionId);

	MealSessionResDto lookupForDate(
			UUID actorUserId,
			UUID donorInstitutionId,
			java.time.LocalDate sessionDate,
			com.mealbridge.domain.donation.entity.MealType mealType);

	MealSessionResDto getById(UUID actorUserId, UUID sessionId);

	MealSessionResDto confirmEvent(UUID actorUserId, UUID sessionId, ConfirmTrackingEventReq request);

	MealSessionResDto close(UUID actorUserId, UUID sessionId);

	void cancelServing(UUID actorUserId, UUID sessionId);
}
