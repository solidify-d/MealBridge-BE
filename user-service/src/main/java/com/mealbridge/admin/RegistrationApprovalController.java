package com.mealbridge.admin;

import com.mealbridge.admin.dto.RejectRegistrationRequest;
import com.mealbridge.admin.service.RegistrationApprovalService;
import com.mealbridge.common.api.ApiResponse;
import com.mealbridge.common.security.SuperAdminOnly;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/registrations")
@RequiredArgsConstructor
@SuperAdminOnly
public class RegistrationApprovalController {

	private final RegistrationApprovalService registrationApprovalService;

	@GetMapping("/pending")
	public ResponseEntity<?> listPending() {
		return ApiResponse.ok(registrationApprovalService.listPending(), "Pending registrations retrieved");
	}

	@GetMapping("/{userId}")
	public ResponseEntity<?> getDetail(@PathVariable UUID userId) {
		return ApiResponse.ok(registrationApprovalService.getDetail(userId), "Registration detail retrieved");
	}

	@PatchMapping("/{userId}/approve")
	public ResponseEntity<?> approve(@PathVariable UUID userId) {
		return ApiResponse.ok(registrationApprovalService.approve(userId), "Registration approved");
	}

	@PatchMapping("/{userId}/reject")
	public ResponseEntity<?> reject(
			@PathVariable UUID userId,
			@RequestBody(required = false) @Valid RejectRegistrationRequest request) {
		String reason = request != null ? request.reason() : null;
		return ApiResponse.ok(registrationApprovalService.reject(userId, reason), "Registration rejected");
	}
}
