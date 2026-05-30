package com.mealbridge.domain.text.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.text.dto.MealEnrichReqDto;
import com.mealbridge.domain.text.service.OpenAiMealEnrichService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/text")
@RequiredArgsConstructor
public class MealEnrichController {

	private final AuthHeaders authHeaders;
	private final OpenAiMealEnrichService openAiMealEnrichService;

	@PostMapping("/enrich-meal-items")
	public ResponseEntity<?> enrichMealItems(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody MealEnrichReqDto request) {

		authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				openAiMealEnrichService.enrich(request),
				"Meal enrichment complete");
	}
}
