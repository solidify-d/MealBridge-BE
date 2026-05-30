package com.mealbridge.common.apiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

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

	public static ResponseEntity<?> created(Object data) {
		return new ResponseEntity<>(new CommonDTO(
				"created success",
				HttpStatus.CREATED.value(),
				data
		), HttpStatus.CREATED);
	}

	public static ResponseEntity<?> created(Object data, String message) {
		return new ResponseEntity<>(new CommonDTO(
				message,
				HttpStatus.CREATED.value(),
				data
		), HttpStatus.CREATED);
	}

	public static ResponseEntity<?> created(Object data, URI location, String message) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.location(location)
				.body(new CommonDTO(message, HttpStatus.CREATED.value(), data));
	}

	public static ResponseEntity<?> noContent() {
		return ResponseEntity.noContent().build();
	}

	public static ResponseEntity<?> unauthorized(String message) {
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(new CommonDTO(message, HttpStatus.UNAUTHORIZED.value(), null));
	}

	public static ResponseEntity<?> forbidden(String message) {
		return ResponseEntity
				.status(HttpStatus.FORBIDDEN)
				.body(new CommonDTO(message, HttpStatus.FORBIDDEN.value(), null));
	}
}
