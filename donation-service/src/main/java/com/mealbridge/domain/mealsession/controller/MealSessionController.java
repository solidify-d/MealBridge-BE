package com.mealbridge.domain.mealsession.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.mealsession.dto.ConfirmTrackingEventReq;
import com.mealbridge.domain.mealsession.dto.CreateMealSessionReq;
import com.mealbridge.domain.mealsession.service.MealSessionService;
import com.mealbridge.domain.mealsession.service.MealStatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/meal-sessions")
@RequiredArgsConstructor
public class MealSessionController {

	private final AuthHeaders authHeaders;
	private final MealSessionService mealSessionService;
	private final MealStatisticsService mealStatisticsService;

	@PostMapping
	public ResponseEntity<?> create(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody CreateMealSessionReq request) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.created(mealSessionService.create(actorId, request), "Meal session started");
	}

	@GetMapping("/statistics")
	public ResponseEntity<?> statistics(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId,
			@RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				mealStatisticsService.mealStatistics(actorId, donorInstitutionId, from, to),
				"Meal statistics");
	}

	@GetMapping("/active")
	public ResponseEntity<?> getActive(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealSessionService.getActive(actorId, donorInstitutionId), "Active meal session");
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getById(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealSessionService.getById(actorId, id), "Meal session retrieved");
	}

	@PostMapping("/{id}/events")
	public ResponseEntity<?> confirmEvent(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody ConfirmTrackingEventReq request) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealSessionService.confirmEvent(actorId, id, request), "Tracking event recorded");
	}

	@PostMapping("/{id}/close")
	public ResponseEntity<?> close(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealSessionService.close(actorId, id), "Meal session closed");
	}
}
