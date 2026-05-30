package com.mealbridge.domain.esg.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.esg.service.EsgReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/esg")
@RequiredArgsConstructor
public class EsgController {

	private final AuthHeaders authHeaders;
	private final EsgReportService esgReportService;

	@GetMapping("/report")
	public ResponseEntity<?> report(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam String month,
			@RequestParam(required = false) UUID donorInstitutionId,
			@RequestParam(required = false) UUID organizationId) {

		authHeaders.requireUserId(userId);
		String actorRole = authHeaders.requireRole(role);
		if (actorRole.equalsIgnoreCase("DONOR")) {
			if (donorInstitutionId == null) {
				throw new IllegalArgumentException("donorInstitutionId is required for donor ESG report");
			}
			authHeaders.requireDonor(userId, role);
			return ApiResponse.ok(
					esgReportService.reportForDonor(donorInstitutionId, month),
					"ESG report retrieved");
		}
		if (actorRole.equalsIgnoreCase("ORGANIZATION")) {
			if (organizationId == null) {
				throw new IllegalArgumentException("organizationId is required for organization ESG report");
			}
			authHeaders.requireOrganization(userId, role);
			return ApiResponse.ok(
					esgReportService.reportForOrganization(organizationId, month),
					"ESG report retrieved");
		}
		throw new SecurityException("ORGANIZATION or DONOR role is required");
	}
}
