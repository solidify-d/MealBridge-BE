package com.mealbridge.domain.weather.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

	private final AuthHeaders authHeaders;
	private final WeatherService weatherService;

	@GetMapping("/current")
	public ResponseEntity<?> current(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donorInstitutionId) {
		authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				weatherService.getCurrentWeather(donorInstitutionId),
				"Current weather retrieved");
	}
}
