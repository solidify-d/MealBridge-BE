package com.mealbridge.domain.vision.dto;

import java.math.BigDecimal;

public record VisionItemEstimate(
		String name,
		BigDecimal kg,
		double confidence
) {
}
