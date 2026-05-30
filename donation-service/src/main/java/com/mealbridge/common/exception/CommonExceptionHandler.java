package com.mealbridge.common.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class CommonExceptionHandler {

	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<?> handleSecurity(SecurityException e) {
		log.warn("[SecurityException] {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(CommonErrorDTO.builder()
						.status_message(e.getMessage())
						.status_code(HttpStatus.FORBIDDEN.value())
						.build());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
		log.error("[IllegalArgumentException] {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(CommonErrorDTO.builder()
						.status_message(e.getMessage())
						.status_code(HttpStatus.BAD_REQUEST.value())
						.build());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
		log.error("[IllegalStateException] {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(CommonErrorDTO.builder()
						.status_message(e.getMessage())
						.status_code(HttpStatus.BAD_REQUEST.value())
						.build());
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<?> handleEntityNotFound(EntityNotFoundException e) {
		log.error("[EntityNotFoundException] {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(CommonErrorDTO.builder()
						.status_message(e.getMessage())
						.status_code(HttpStatus.NOT_FOUND.value())
						.build());
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<?> handleNoSuchElement(NoSuchElementException e) {
		log.error("[NoSuchElementException] {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(CommonErrorDTO.builder()
						.status_message(e.getMessage())
						.status_code(HttpStatus.NOT_FOUND.value())
						.build());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(CommonErrorDTO.builder()
						.status_message(message)
						.status_code(HttpStatus.BAD_REQUEST.value())
						.build());
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<?> handleDuplicateResource(DuplicateResourceException e) {
		log.error("[DuplicateResourceException] {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(CommonErrorDTO.builder()
						.status_message(e.getMessage())
						.status_code(HttpStatus.BAD_REQUEST.value())
						.result(e.getData())
						.build());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
		String rootMessage = e.getMessage();
		if (rootMessage != null && rootMessage.contains("UUID")) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(CommonErrorDTO.builder()
							.status_code(HttpStatus.BAD_REQUEST.value())
							.status_message("Request body contains an invalid UUID.")
							.build());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(CommonErrorDTO.builder()
						.status_code(HttpStatus.BAD_REQUEST.value())
						.status_message("Invalid request body.")
						.build());
	}
}
