package com.mealbridge.domain.vision.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.vision.VisionMode;
import com.mealbridge.domain.vision.service.BedrockVisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/vision")
@RequiredArgsConstructor
public class VisionAnalyzeController {

	private final AuthHeaders authHeaders;
	private final BedrockVisionService bedrockVisionService;

	@PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> analyze(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam("mode") String modeParam,
			@RequestParam(value = "sessionContext", required = false) String sessionContext,
			@RequestPart(value = "image", required = false) MultipartFile image) {

		authHeaders.requireDonor(userId, role);
		VisionMode mode = parseMode(modeParam);
		if (image == null || image.isEmpty()) {
			throw new IllegalArgumentException("Multipart field 'image' is required and must not be empty");
		}
		return ApiResponse.ok(
				bedrockVisionService.analyze(mode, image, sessionContext),
				"Vision analysis complete");
	}

	private static VisionMode parseMode(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException(
					"Query parameter 'mode' is required (SERVING, LEFTOVER, MEAL_REGISTER)");
		}
		try {
			return VisionMode.valueOf(raw.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(
					"Invalid mode '" + raw + "'. Allowed: SERVING, LEFTOVER, MEAL_REGISTER");
		}
	}
}
