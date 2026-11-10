package com.mealbridge.domain.registration.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.registration.service.query.RegistrationApprovalQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/registrations")
@RequiredArgsConstructor
public class RegistrationApprovalQueryController {

	private final AuthHeaders authHeaders;
	private final RegistrationApprovalQueryService registrationApprovalQueryService;

	@GetMapping("/pending")
	public ResponseEntity<?> listPending(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		authHeaders.requireActiveSuperAdmin(userId, role);
		return ApiResponse.ok(registrationApprovalQueryService.listPending(), "Pending registrations retrieved");
	}

	@GetMapping("/{userId}")
	public ResponseEntity<?> getDetail(
			@RequestHeader(value = "X-User-Id", required = false) String actorId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID userId) {
		authHeaders.requireActiveSuperAdmin(actorId, role);
		return ApiResponse.ok(registrationApprovalQueryService.getDetail(userId), "Registration detail retrieved");
	}
}
