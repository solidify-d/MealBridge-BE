package com.mealbridge.common.test.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

	@GetMapping("/health")
	public ResponseEntity<?> healthCheck() {
		return ApiResponse.ok("matching-service: OK");
	}
}
