package com.mealbridge.domain.donation.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
public class DonationMeQueryController {

	private final AuthHeaders authHeaders;

	@GetMapping("/me")
	public ResponseEntity<?> myDonations(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {

		var actorId = authHeaders.requireOrganizationOrDonor(userId, role);
		return ApiResponse.ok(Map.of(
				"userId", actorId,
				"role", role.toUpperCase(),
				"message", "Donation service received gateway auth headers"
		), "Donation profile retrieved");
	}
}
