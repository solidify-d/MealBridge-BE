package com.mealbridge.common.test.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> healthCheck() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status_message", "success");
		body.put("status_code", HttpStatus.OK.value());
		body.put("result", "apigateway: OK");
		return ResponseEntity.ok(body);
	}
}
