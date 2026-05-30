package com.mealbridge.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ChatReqDto(
		@NotBlank String message,
		UUID conversationId,
		UUID donorInstitutionId,
		UUID organizationId,
		String contextJson
) {
}
