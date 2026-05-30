package com.mealbridge.domain.mealplan.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealplan.dto.BulkMealPlanReq;
import com.mealbridge.domain.mealplan.dto.UpsertMealPlanReq;
import com.mealbridge.domain.mealplan.service.MealPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {

	private final AuthHeaders authHeaders;
	private final MealPlanService mealPlanService;

	@GetMapping
	public ResponseEntity<?> list(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				mealPlanService.list(actorId, donorInstitutionId, from, to),
				"Meal plans retrieved");
	}

	@GetMapping("/lookup")
	public ResponseEntity<?> lookup(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
			@RequestParam MealType mealType) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				mealPlanService.getForDate(actorId, donorInstitutionId, planDate, mealType),
				"Meal plan retrieved");
	}

	@PostMapping
	public ResponseEntity<?> upsert(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody UpsertMealPlanReq request) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(mealPlanService.upsert(actorId, request), "Meal plan saved");
	}

	@PostMapping("/bulk")
	public ResponseEntity<?> bulk(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody BulkMealPlanReq request) {
		UUID actorId = authHeaders.requireDonor(userId, role);
		return ApiResponse.created(mealPlanService.bulkUpsert(actorId, request), "Meal plans imported");
	}
}
