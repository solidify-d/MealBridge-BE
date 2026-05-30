package com.mealbridge.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ApiResponse {

	private ApiResponse() {
	}

	public static <T> ResponseEntity<ApiResponseBody<T>> ok(T data, String message) {
		return ResponseEntity.ok(new ApiResponseBody<>(HttpStatus.OK.value(), message, data));
	}

	public static <T> ResponseEntity<ApiResponseBody<T>> error(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(new ApiResponseBody<>(status.value(), message, null));
	}
}
