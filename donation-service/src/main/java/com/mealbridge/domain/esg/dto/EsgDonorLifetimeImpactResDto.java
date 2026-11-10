package com.mealbridge.domain.esg.dto;

import java.math.BigDecimal;

/**
 * Cumulative donor impact since the first month with ESG activity on the platform.
 */
public record EsgDonorLifetimeImpactResDto(
		String sinceMonth,
		BigDecimal wasteReducedKg,
		BigDecimal surplusDonatedKg,
		BigDecimal wasteDisposalAvoidedKg,
		BigDecimal carbonReductionKg,
		BigDecimal disposalCostPerKgKrw,
		BigDecimal disposalCostSavedKrw) {
}
