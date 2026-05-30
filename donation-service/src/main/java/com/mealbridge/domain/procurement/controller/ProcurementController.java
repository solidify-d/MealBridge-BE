package com.mealbridge.domain.procurement.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.procurement.service.ProcurementSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/procurement")
@RequiredArgsConstructor
public class ProcurementController {

	private final AuthHeaders authHeaders;
	private final ProcurementSuggestionService procurementSuggestionService;

	@GetMapping("/suggestions")
	public ResponseEntity<?> suggestions(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		authHeaders.requireDonor(userId, role);
		LocalDate start = from != null ? from : KstDates.today();
		LocalDate end = to != null ? to : start.plusDays(7);
		return ApiResponse.ok(
				procurementSuggestionService.suggest(donorInstitutionId, start, end),
				"Procurement suggestions generated");
	}
}
