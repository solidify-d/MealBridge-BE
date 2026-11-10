package com.mealbridge.domain.esg.dto;

import com.mealbridge.domain.esg.entity.EsgActorType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EsgReportResDto(
		String month,
		EsgActorType actorType,
		UUID actorId,
		BigDecimal totalWeightKg,
		BigDecimal totalCo2eKg,
		List<EsgEventBreakdownResDto> breakdown,
		String methodologyDisclaimer,
		EsgDonorImpactResDto donorImpact,
		EsgDonorLifetimeImpactResDto donorLifetimeImpact) {
}
