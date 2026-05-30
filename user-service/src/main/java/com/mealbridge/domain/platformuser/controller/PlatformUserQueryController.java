package com.mealbridge.domain.platformuser.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.platformuser.service.query.PlatformUserQueryService;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/platform-users")
@RequiredArgsConstructor
public class PlatformUserQueryController {

	private final AuthHeaders authHeaders;
	private final PlatformUserQueryService platformUserQueryService;

	@GetMapping
	public ResponseEntity<?> listAll(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam(required = false) Role filterRole,
			@RequestParam(required = false) UserStatus filterStatus) {
		authHeaders.requireActiveSuperAdmin(userId, role);
		return ApiResponse.ok(
				platformUserQueryService.listAll(filterRole, filterStatus),
				"Platform users retrieved");
	}

	@GetMapping("/{userId}")
	public ResponseEntity<?> getDetail(
			@RequestHeader(value = "X-User-Id", required = false) String actorId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable UUID userId) {
		authHeaders.requireActiveSuperAdmin(actorId, role);
		return ApiResponse.ok(platformUserQueryService.getDetail(userId), "Platform user detail retrieved");
	}
}
