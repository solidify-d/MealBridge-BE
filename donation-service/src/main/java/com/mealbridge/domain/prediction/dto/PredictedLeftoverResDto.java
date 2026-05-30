package com.mealbridge.domain.prediction.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PredictedLeftoverResDto(
		BigDecimal predictedWasteKg,
		BigDecimal predictedWastePerPersonKg,
		Integer referenceSessionCount,
		String disclaimer,
		List<SimilarSessionRef> similarSessions) {

	public record SimilarSessionRef(
			UUID sessionId,
			String sessionDate,
			double similarityScore,
			BigDecimal wastePerPersonKg) {
	}
}
