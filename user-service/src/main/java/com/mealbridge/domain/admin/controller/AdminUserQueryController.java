package com.mealbridge.domain.admin.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.admin.service.query.AdminUserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserQueryController {

	private final AuthHeaders authHeaders;
	private final AdminUserQueryService adminUserQueryService;

	@GetMapping
	public ResponseEntity<?> listAdmins(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		authHeaders.requireActiveSuperAdmin(userId, role);
		return ApiResponse.ok(adminUserQueryService.listAdmins(), "Admin users retrieved");
	}

	@GetMapping("/me")
	public ResponseEntity<?> me(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		var actorId = authHeaders.requireActiveAdminPortalUser(userId, role);
		return ApiResponse.ok(adminUserQueryService.getMe(actorId), "Current admin profile");
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getById(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID id) {
		authHeaders.requireActiveSuperAdmin(userId, role);
		return ApiResponse.ok(adminUserQueryService.getById(id), "Admin user retrieved");
	}
}
