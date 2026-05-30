package com.mealbridge.domain.organization.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.organization.service.query.OrganizationProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/organization")
@RequiredArgsConstructor
public class OrganizationProfileQueryController {

	private final AuthHeaders authHeaders;
	private final OrganizationProfileQueryService organizationProfileQueryService;

	@GetMapping("/profile")
	public ResponseEntity<?> profile(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		var actorId = authHeaders.requireOrganization(userId, role);
		var body = organizationProfileQueryService.getProfile(actorId);
		return ApiResponse.ok(body, "Organization profile retrieved");
	}
}
