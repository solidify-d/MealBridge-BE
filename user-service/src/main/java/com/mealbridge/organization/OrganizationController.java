package com.mealbridge.organization;

import com.mealbridge.common.api.ApiResponse;
import com.mealbridge.common.security.CustomUserPrincipal;
import com.mealbridge.common.security.OrganizationOnly;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/organization")
@OrganizationOnly
public class OrganizationController {

	@GetMapping("/profile")
	public ResponseEntity<?> profile(@AuthenticationPrincipal CustomUserPrincipal principal) {
		return ApiResponse.ok(Map.of(
				"userId", principal.userId(),
				"role", principal.role().name(),
				"message", "Organization protected endpoint"
		), "OK");
	}
}
