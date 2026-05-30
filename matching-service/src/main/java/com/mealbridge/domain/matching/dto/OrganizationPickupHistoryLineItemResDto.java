package com.mealbridge.domain.matching.dto;

import java.math.BigDecimal;

public record OrganizationPickupHistoryLineItemResDto(
		String itemName,
		BigDecimal preparedKg,
		BigDecimal perPersonKg,
		String ingredients,
		String nutritionJson,
		String allergensJson) {
}
