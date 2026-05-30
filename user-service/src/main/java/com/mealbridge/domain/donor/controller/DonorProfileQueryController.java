package com.mealbridge.domain.donor.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.donor.service.query.DonorProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/donor")
@RequiredArgsConstructor
public class DonorProfileQueryController {

	private final AuthHeaders authHeaders;
	private final DonorProfileQueryService donorProfileQueryService;

	@GetMapping("/profile")
	public ResponseEntity<?> profile(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		var actorId = authHeaders.requireDonor(userId, role);
		var body = donorProfileQueryService.getProfile(actorId);
		return ApiResponse.ok(body, "Donor profile retrieved");
	}
}
