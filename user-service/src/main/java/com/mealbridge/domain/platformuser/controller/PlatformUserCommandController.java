package com.mealbridge.domain.platformuser.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.platformuser.dto.UpdatePlatformUserStatusReq;
import com.mealbridge.domain.platformuser.service.command.PlatformUserCommandService;
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
@RequestMapping("/admin/platform-users")
@RequiredArgsConstructor
public class PlatformUserCommandController {

	private final AuthHeaders authHeaders;
	private final PlatformUserCommandService platformUserCommandService;

	@PatchMapping("/{userId}/status")
	public ResponseEntity<?> updateStatus(
			@RequestHeader(value = "X-User-Id", required = false) String actorId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID userId,
			@Valid @RequestBody UpdatePlatformUserStatusReq request) {
		authHeaders.requireActiveSuperAdmin(actorId, role);
		var res = platformUserCommandService.updateStatus(userId, request.status(), request.reason());
		return ApiResponse.ok(res, "User status updated");
	}
}
