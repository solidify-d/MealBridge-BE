package com.mealbridge.domain.procurement.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.procurement.dto.ProcurementAdviseReqDto;
import com.mealbridge.domain.procurement.service.ProcurementAdvisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/procurement")
@RequiredArgsConstructor
public class ProcurementAdvisorController {

	private final AuthHeaders authHeaders;
	private final ProcurementAdvisorService procurementAdvisorService;

	@PostMapping("/advise")
	public ResponseEntity<?> advise(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody ProcurementAdviseReqDto request) {
		authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(procurementAdvisorService.advise(request), "Procurement AI advice generated");
	}
}
