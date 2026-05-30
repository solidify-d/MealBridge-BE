package com.mealbridge.domain.mealsession.service;

import com.mealbridge.domain.mealsession.dto.ConfirmTrackingEventReq;
import com.mealbridge.domain.mealsession.dto.CreateMealSessionReq;
import com.mealbridge.domain.mealsession.dto.MealSessionResDto;

import java.util.UUID;

public interface MealSessionService {

	MealSessionResDto create(UUID actorUserId, CreateMealSessionReq request);

	MealSessionResDto getActive(UUID actorUserId, UUID donorInstitutionId);

	MealSessionResDto getById(UUID actorUserId, UUID sessionId);

	MealSessionResDto confirmEvent(UUID actorUserId, UUID sessionId, ConfirmTrackingEventReq request);

	MealSessionResDto close(UUID actorUserId, UUID sessionId);
}
