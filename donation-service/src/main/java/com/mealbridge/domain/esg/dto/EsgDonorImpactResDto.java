package com.mealbridge.domain.esg.dto;

import java.math.BigDecimal;

/**
 * Donor-only impact summary: food served instead of wasted plus surplus donated,
 * converted to landfill diversion and CO₂e using {@link com.mealbridge.domain.esg.service.EsgAggregationService#CO2E_FACTOR_KG_PER_KG}.
 */
public record EsgDonorImpactResDto(
		BigDecimal carbonReductionKg,
		BigDecimal wasteDisposalAvoidedKg,
		BigDecimal wasteReducedKg,
		BigDecimal surplusDonatedKg) {
}
