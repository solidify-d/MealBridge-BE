package com.mealbridge.domain.registration.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.registration.dto.RejectRegistrationReq;
import com.mealbridge.domain.registration.service.command.RegistrationApprovalCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/registrations")
@RequiredArgsConstructor
public class RegistrationApprovalCommandController {

	private final AuthHeaders authHeaders;
	private final RegistrationApprovalCommandService registrationApprovalCommandService;

	@PatchMapping("/{userId}/approve")
	public ResponseEntity<?> approve(
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID userId) {
		authHeaders.requireSuperAdmin(role);
		return ApiResponse.ok(registrationApprovalCommandService.approve(userId), "Registration approved");
	}

	@PatchMapping("/{userId}/reject")
	public ResponseEntity<?> reject(
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID userId,
			@RequestBody(required = false) @Valid RejectRegistrationReq request) {
		authHeaders.requireSuperAdmin(role);
		String reason = request != null ? request.reason() : null;
		return ApiResponse.ok(registrationApprovalCommandService.reject(userId, reason), "Registration rejected");
	}
}
