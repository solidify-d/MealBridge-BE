package com.mealbridge.common.apiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ApiResponse {

	private ApiResponse() {
	}

	public static ResponseEntity<?> ok(Object data) {
		return new ResponseEntity<>(new CommonDTO(
				"success",
				HttpStatus.OK.value(),
				data
		), HttpStatus.OK);
	}

	public static ResponseEntity<?> ok(Object data, String message) {
		return new ResponseEntity<>(new CommonDTO(
				message,
				HttpStatus.OK.value(),
				data
		), HttpStatus.OK);
	}
}
