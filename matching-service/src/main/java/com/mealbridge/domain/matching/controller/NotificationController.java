package com.mealbridge.domain.matching.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.matching.service.NotificationService;
import com.mealbridge.domain.matching.service.NotificationSseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final AuthHeaders authHeaders;
	private final NotificationService notificationService;
	private final NotificationSseHub notificationSseHub;

	@GetMapping
	public ResponseEntity<?> list(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		UUID actorId = authHeaders.requireAuthenticated(userId, role);
		return ApiResponse.ok(notificationService.list(actorId, page, size), "Notifications retrieved");
	}

	@GetMapping("/unread-count")
	public ResponseEntity<?> unreadCount(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireAuthenticated(userId, role);
		return ApiResponse.ok(notificationService.unreadCount(actorId), "Unread count retrieved");
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<?> markRead(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireAuthenticated(userId, role);
		return ApiResponse.ok(notificationService.markRead(actorId, id), "Notification marked read");
	}

	@PostMapping("/read-all")
	public ResponseEntity<?> markAllRead(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireAuthenticated(userId, role);
		notificationService.markAllRead(actorId);
		return ApiResponse.ok(null, "All notifications marked read");
	}

	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		UUID actorId = authHeaders.requireAuthenticated(userId, role);
		return notificationSseHub.subscribe(actorId);
	}
}
