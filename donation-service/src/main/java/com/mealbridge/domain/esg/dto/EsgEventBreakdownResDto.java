package com.mealbridge.domain.esg.dto;

import com.mealbridge.domain.esg.entity.EsgEventType;

import java.math.BigDecimal;

public record EsgEventBreakdownResDto(
		EsgEventType eventType,
		BigDecimal weightKg,
		BigDecimal co2eKg,
		int entryCount) {
}
