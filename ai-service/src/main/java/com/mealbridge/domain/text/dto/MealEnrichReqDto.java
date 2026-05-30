package com.mealbridge.domain.text.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MealEnrichReqDto(
		@NotEmpty @Valid List<MealEnrichItemReq> items
) {
}
