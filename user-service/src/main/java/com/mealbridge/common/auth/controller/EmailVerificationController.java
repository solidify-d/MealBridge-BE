package com.mealbridge.common.auth.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.dto.EmailVerificationConfirmRequest;
import com.mealbridge.common.auth.dto.EmailVerificationResponse;
import com.mealbridge.common.auth.dto.EmailVerificationSendRequest;
import com.mealbridge.common.auth.email.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/email/verification")
@RequiredArgsConstructor
public class EmailVerificationController {

	private final EmailVerificationService emailVerificationService;

	@PostMapping("/send")
	public ResponseEntity<?> send(@Valid @RequestBody EmailVerificationSendRequest request) {
		emailVerificationService.sendVerificationCode(request.email());
		var body = EmailVerificationResponse.sent("Verification code sent to your email");
		return ApiResponse.ok(body, body.message());
	}

	@PostMapping("/confirm")
	public ResponseEntity<?> confirm(@Valid @RequestBody EmailVerificationConfirmRequest request) {
		emailVerificationService.confirmVerificationCode(request.email(), request.code());
		var body = EmailVerificationResponse.confirmed("Email verified. You can complete registration.");
		return ApiResponse.ok(body, body.message());
	}
}
