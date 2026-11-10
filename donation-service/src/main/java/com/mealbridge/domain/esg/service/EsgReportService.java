package com.mealbridge.domain.esg.service;

import com.mealbridge.domain.esg.dto.EsgDonorImpactResDto;
import com.mealbridge.domain.esg.dto.EsgDonorLifetimeImpactResDto;
import com.mealbridge.domain.esg.dto.EsgEventBreakdownResDto;
import com.mealbridge.domain.esg.dto.EsgReportResDto;
import com.mealbridge.domain.esg.entity.EsgActorType;
import com.mealbridge.domain.esg.entity.EsgEventType;
import com.mealbridge.domain.esg.repository.EsgContributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EsgReportService {

	private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

	public static final String METHODOLOGY_DISCLAIMER =
			"CO₂e estimates use the EPA WARM model low-end factor of 0.49 kg CO₂e per kg of food "
					+ "waste avoided or surplus redirected. Actual emissions may vary by disposal path, "
					+ "transport, and menu composition.";

	public static final String DONOR_METHODOLOGY_DISCLAIMER =
			METHODOLOGY_DISCLAIMER
					+ " Waste disposal avoided counts food served to diners (waste reduced) plus surplus "
					+ "donated instead of landfilled, in kg. Carbon reduction is the same mass × 0.49 kg CO₂e/kg. "
					+ "Disposal cost savings use a reference rate of ₩210/kg (army pilot disposal spend ÷ annual "
					+ "food waste). Lifetime totals start from your first month with recorded ESG activity.";

	/** Reference food-waste disposal unit cost (KRW/kg), derived from army pilot figures (~₩18.9B / 90k t). */
	public static final BigDecimal WASTE_DISPOSAL_COST_KRW_PER_KG = new BigDecimal("210");

	private final EsgContributionRepository esgContributionRepository;

	public EsgReportResDto reportForDonor(UUID donorInstitutionId, String month) {
		validateMonth(month);
		EsgReportResDto report = buildReport(EsgActorType.DONOR, donorInstitutionId, month);
		return withDonorImpact(report, donorInstitutionId);
	}

	public EsgReportResDto reportForOrganization(UUID organizationId, String month) {
		validateMonth(month);
		return buildReport(EsgActorType.ORGANIZATION, organizationId, month, null, null, METHODOLOGY_DISCLAIMER);
	}

	private EsgReportResDto withDonorImpact(EsgReportResDto report, UUID donorInstitutionId) {
		BigDecimal wasteReduced = weightForEvent(report, EsgEventType.WASTE_REDUCED);
		BigDecimal surplusDonated = weightForEvent(report, EsgEventType.SURPLUS_DONATED);
		BigDecimal disposalAvoided = wasteReduced.add(surplusDonated).setScale(3, RoundingMode.HALF_UP);
		EsgDonorImpactResDto impact = new EsgDonorImpactResDto(
				report.totalCo2eKg(),
				disposalAvoided,
				wasteReduced,
				surplusDonated);
		EsgDonorLifetimeImpactResDto lifetimeImpact = buildLifetimeImpact(donorInstitutionId);
		return new EsgReportResDto(
				report.month(),
				report.actorType(),
				report.actorId(),
				report.totalWeightKg(),
				report.totalCo2eKg(),
				report.breakdown(),
				DONOR_METHODOLOGY_DISCLAIMER,
				impact,
				lifetimeImpact);
	}

	private EsgDonorLifetimeImpactResDto buildLifetimeImpact(UUID donorInstitutionId) {
		Optional<String> sinceMonthOpt =
				esgContributionRepository.findFirstMonth(EsgActorType.DONOR, donorInstitutionId);
		if (sinceMonthOpt.isEmpty()) {
			return null;
		}
		String sinceMonth = sinceMonthOpt.get();
		List<Object[]> aggregates =
				esgContributionRepository.aggregateByEventTypeFromMonth(
						EsgActorType.DONOR, donorInstitutionId, sinceMonth);

		BigDecimal wasteReduced = weightForEventType(aggregates, EsgEventType.WASTE_REDUCED);
		BigDecimal surplusDonated = weightForEventType(aggregates, EsgEventType.SURPLUS_DONATED);
		BigDecimal disposalAvoided = wasteReduced.add(surplusDonated).setScale(3, RoundingMode.HALF_UP);
		BigDecimal carbonReduction = co2eForEventType(aggregates, EsgEventType.WASTE_REDUCED)
				.add(co2eForEventType(aggregates, EsgEventType.SURPLUS_DONATED))
				.setScale(3, RoundingMode.HALF_UP);
		BigDecimal disposalCostSaved = disposalAvoided
				.multiply(WASTE_DISPOSAL_COST_KRW_PER_KG)
				.setScale(0, RoundingMode.HALF_UP);

		return new EsgDonorLifetimeImpactResDto(
				sinceMonth,
				wasteReduced,
				surplusDonated,
				disposalAvoided,
				carbonReduction,
				WASTE_DISPOSAL_COST_KRW_PER_KG,
				disposalCostSaved);
	}

	private BigDecimal weightForEventType(List<Object[]> aggregates, EsgEventType type) {
		return aggregates.stream()
				.filter(row -> row[0] == type)
				.map(row -> toBigDecimal(row[1]))
				.findFirst()
				.orElse(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
	}

	private BigDecimal co2eForEventType(List<Object[]> aggregates, EsgEventType type) {
		return aggregates.stream()
				.filter(row -> row[0] == type)
				.map(row -> toBigDecimal(row[2]))
				.findFirst()
				.orElse(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
	}

	private BigDecimal weightForEvent(EsgReportResDto report, EsgEventType type) {
		return report.breakdown().stream()
				.filter(row -> row.eventType() == type)
				.map(EsgEventBreakdownResDto::weightKg)
				.findFirst()
				.orElse(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
	}

	private EsgReportResDto buildReport(EsgActorType actorType, UUID actorId, String month) {
		return buildReport(actorType, actorId, month, null, null, METHODOLOGY_DISCLAIMER);
	}

	private EsgReportResDto buildReport(
			EsgActorType actorType,
			UUID actorId,
			String month,
			EsgDonorImpactResDto donorImpact,
			EsgDonorLifetimeImpactResDto donorLifetimeImpact,
			String disclaimer) {
		List<Object[]> aggregates = esgContributionRepository.aggregateByEventType(actorType, actorId, month);
		Map<EsgEventType, EsgEventBreakdownResDto> breakdownByType = new EnumMap<>(EsgEventType.class);
		for (Object[] row : aggregates) {
			EsgEventType eventType = (EsgEventType) row[0];
			BigDecimal weightKg = toBigDecimal(row[1]);
			BigDecimal co2eKg = toBigDecimal(row[2]);
			int entryCount = countEntries(actorType, actorId, month, eventType);
			breakdownByType.put(
					eventType,
					new EsgEventBreakdownResDto(eventType, weightKg, co2eKg, entryCount));
		}

		List<EsgEventBreakdownResDto> breakdown = new ArrayList<>();
		BigDecimal totalWeight = BigDecimal.ZERO;
		BigDecimal totalCo2e = BigDecimal.ZERO;
		for (EsgEventType type : EsgEventType.values()) {
			EsgEventBreakdownResDto item = breakdownByType.getOrDefault(
					type,
					new EsgEventBreakdownResDto(type, BigDecimal.ZERO.setScale(3), BigDecimal.ZERO.setScale(3), 0));
			breakdown.add(item);
			totalWeight = totalWeight.add(item.weightKg());
			totalCo2e = totalCo2e.add(item.co2eKg());
		}

		return new EsgReportResDto(
				month,
				actorType,
				actorId,
				totalWeight.setScale(3, RoundingMode.HALF_UP),
				totalCo2e.setScale(3, RoundingMode.HALF_UP),
				breakdown,
				disclaimer,
				donorImpact,
				donorLifetimeImpact);
	}

	private int countEntries(EsgActorType actorType, UUID actorId, String month, EsgEventType eventType) {
		return Math.toIntExact(
				esgContributionRepository.countByActorTypeAndActorIdAndMonthAndEventType(
						actorType, actorId, month, eventType));
	}

	private BigDecimal toBigDecimal(Object value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
		}
		if (value instanceof BigDecimal bd) {
			return bd.setScale(3, RoundingMode.HALF_UP);
		}
		return new BigDecimal(value.toString()).setScale(3, RoundingMode.HALF_UP);
	}

	private void validateMonth(String month) {
		if (month == null || !MONTH_PATTERN.matcher(month).matches()) {
			throw new IllegalArgumentException("month must be YYYY-MM");
		}
	}
}
