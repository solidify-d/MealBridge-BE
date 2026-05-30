package com.mealbridge.domain.mealsession.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealsession.dto.ConfirmTrackingEventReq;
import com.mealbridge.domain.mealsession.dto.CreateMealSessionReq;
import com.mealbridge.domain.analytics.service.MealAnalyticsService;
import com.mealbridge.domain.mealsession.service.MealSessionService;
import com.mealbridge.domain.mealsession.service.MealStatisticsService;
import com.mealbridge.domain.prediction.dto.PredictedLeftoverResDto;
import com.mealbridge.domain.prediction.service.LeftoverPredictionService;
import com.mealbridge.domain.mealsession.dto.LineItemInput;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/meal-sessions")
@RequiredArgsConstructor
public class MealSessionController {

	private static final String SESSION_ID =
			"{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}";

	private final AuthHeaders authHeaders;
	private final MealSessionService mealSessionService;
	private final MealStatisticsService mealStatisticsService;
	private final MealAnalyticsService mealAnalyticsService;
	private final LeftoverPredictionService leftoverPredictionService;

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

	@GetMapping("/analytics")
	public ResponseEntity<?> analytics(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId,
			@RequestParam LocalDate from,
			@RequestParam LocalDate to,
			@RequestParam(defaultValue = "day") String groupBy) {
		authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				mealAnalyticsService.analytics(donorInstitutionId, from, to, groupBy),
				"Meal analytics");
	}

	@PostMapping("/predict-leftover")
	public ResponseEntity<?> predictLeftover(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId,
			@RequestParam LocalDate sessionDate,
			@RequestParam Integer headcount,
			@RequestParam(required = false) Integer weatherCode,
			@RequestParam(required = false) Double temperatureC,
			@RequestParam(required = false) Integer humidityPct,
			@Valid @RequestBody List<LineItemInput> lineItems) {
		authHeaders.requireDonor(userId, role);
		PredictedLeftoverResDto prediction = leftoverPredictionService.predict(
				donorInstitutionId, sessionDate, headcount, weatherCode, temperatureC, humidityPct, lineItems);
		return ApiResponse.ok(prediction, "Leftover prediction");
	}

	@GetMapping("/active")
	public ResponseEntity<?> getActive(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealSessionService.getActive(actorId, donorInstitutionId), "Active meal session");
	}

	@GetMapping("/lookup")
	public ResponseEntity<?> lookup(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
			@RequestParam MealType mealType) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				mealSessionService.lookupForDate(actorId, donorInstitutionId, sessionDate, mealType),
				"Meal session retrieved");
	}

	@GetMapping("/" + SESSION_ID)
	public ResponseEntity<?> getById(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealSessionService.getById(actorId, id), "Meal session retrieved");
	}

	@PostMapping("/" + SESSION_ID + "/events")
	public ResponseEntity<?> confirmEvent(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody ConfirmTrackingEventReq request) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealSessionService.confirmEvent(actorId, id, request), "Tracking event recorded");
	}

	@PostMapping("/" + SESSION_ID + "/close")
	public ResponseEntity<?> close(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealSessionService.close(actorId, id), "Meal session closed");
	}

	@DeleteMapping("/" + SESSION_ID)
	public ResponseEntity<?> cancelServing(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		mealSessionService.cancelServing(actorId, id);
		return ApiResponse.ok(null, "Serving cancelled");
	}
}
