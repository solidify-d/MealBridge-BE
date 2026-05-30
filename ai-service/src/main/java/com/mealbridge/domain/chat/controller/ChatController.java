package com.mealbridge.domain.chat.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.chat.dto.ChatReqDto;
import com.mealbridge.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

	private final AuthHeaders authHeaders;
	private final ChatService chatService;

	@PostMapping
	public ResponseEntity<?> chat(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody ChatReqDto request) {

		authHeaders.requireDonorOrOrganization(userId, role);
		return ApiResponse.ok(chatService.chat(request), "Chat reply generated");
	}
}
