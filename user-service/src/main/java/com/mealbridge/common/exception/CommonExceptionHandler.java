package com.mealbridge.common.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

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

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<?> handleUnauthorized(UnauthorizedException e) {
		log.warn("[UnauthorizedException] {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(CommonErrorDTO.builder()
						.status_message(e.getMessage())
						.status_code(HttpStatus.UNAUTHORIZED.value())
						.build());
	}

	@ExceptionHandler(AccountSuspendedException.class)
	public ResponseEntity<?> handleAccountSuspended(AccountSuspendedException e) {
		log.warn("[AccountSuspendedException] {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(CommonErrorDTO.builder()
						.status_message("Account suspended")
						.status_code(HttpStatus.FORBIDDEN.value())
						.result(e.getInfo())
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
		log.error("[MethodArgumentNotValidException] {}", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(CommonErrorDTO.builder()
						.status_message(message)
						.status_code(HttpStatus.BAD_REQUEST.value())
						.build());
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<?> handleNoHandlerFound(NoHandlerFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(CommonErrorDTO.builder()
						.status_message("Resource not found")
						.status_code(HttpStatus.NOT_FOUND.value())
						.build());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		String param = e.getName() != null ? e.getName() : "parameter";
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(CommonErrorDTO.builder()
						.status_message("Invalid " + param)
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
			log.warn("[HttpMessageNotReadableException - UUID] {}", rootMessage);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(CommonErrorDTO.builder()
							.status_code(HttpStatus.BAD_REQUEST.value())
							.status_message("Request body contains an invalid UUID.")
							.build());
		}
		log.warn("[HttpMessageNotReadableException] {}", rootMessage);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(CommonErrorDTO.builder()
						.status_code(HttpStatus.BAD_REQUEST.value())
						.status_message("Invalid request body.")
						.build());
	}
}
