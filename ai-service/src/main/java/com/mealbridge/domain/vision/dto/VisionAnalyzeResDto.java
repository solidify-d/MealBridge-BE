package com.mealbridge.domain.vision.dto;

import com.mealbridge.domain.vision.VisionMode;

import java.math.BigDecimal;
import java.util.List;

public record VisionAnalyzeResDto(
		VisionMode mode,
		BigDecimal totalDeltaKg,
		List<VisionItemEstimate> items,
		String rawResponse,
		boolean mock
) {
}
