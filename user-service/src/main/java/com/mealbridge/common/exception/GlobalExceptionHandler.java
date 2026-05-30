package com.mealbridge.common.exception;

import com.mealbridge.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex) {
		return ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
		return ApiResponse.error(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.orElse("Validation failed");
		return ApiResponse.error(HttpStatus.BAD_REQUEST, message);
	}
}
