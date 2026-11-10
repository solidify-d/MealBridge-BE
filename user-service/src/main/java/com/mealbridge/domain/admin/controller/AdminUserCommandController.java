package com.mealbridge.domain.admin.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.admin.dto.CreateAdminReq;
import com.mealbridge.domain.admin.dto.UpdateAdminReq;
import com.mealbridge.domain.admin.dto.UpdateAdminStatusReq;
import com.mealbridge.domain.admin.service.command.AdminUserCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserCommandController {

	private final AuthHeaders authHeaders;
	private final AdminUserCommandService adminUserCommandService;

	@PostMapping
	public ResponseEntity<?> createAdmin(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody CreateAdminReq request) {
		var actorId = authHeaders.requireActiveSuperAdmin(userId, role);
		var res = adminUserCommandService.createAdmin(request, actorId);
		return ApiResponse.created(res, "Admin user created");
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> updateAdmin(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID id,
			@Valid @RequestBody UpdateAdminReq request) {
		authHeaders.requireActiveSuperAdmin(userId, role);
		var res = adminUserCommandService.updateAdmin(id, request);
		return ApiResponse.ok(res, "Admin user updated");
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<?> updateStatus(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID id,
			@Valid @RequestBody UpdateAdminStatusReq request) {
		authHeaders.requireActiveSuperAdmin(userId, role);
		var res = adminUserCommandService.updateStatus(id, request.status(), request.reason());
		return ApiResponse.ok(res, "Admin status updated");
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteAdmin(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID id) {
		authHeaders.requireActiveSuperAdmin(userId, role);
		adminUserCommandService.deleteAdmin(id);
		return ApiResponse.ok(null, "Admin user deleted");
	}
}
