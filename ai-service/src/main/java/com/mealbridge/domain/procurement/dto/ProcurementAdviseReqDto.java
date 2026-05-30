package com.mealbridge.domain.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProcurementAdviseReqDto(
		@NotNull UUID donorInstitutionId,
		@NotBlank String suggestionsJson) {
}
