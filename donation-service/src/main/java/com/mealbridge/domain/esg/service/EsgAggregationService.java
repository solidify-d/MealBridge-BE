package com.mealbridge.domain.esg.service;

import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.esg.entity.EsgActorType;
import com.mealbridge.domain.esg.entity.EsgContribution;
import com.mealbridge.domain.esg.entity.EsgEventType;
import com.mealbridge.domain.esg.repository.EsgContributionRepository;
import com.mealbridge.domain.mealsession.entity.MealSession;
import com.mealbridge.domain.mealsession.entity.TrackingEvent;
import com.mealbridge.domain.mealsession.entity.TrackingMode;
import com.mealbridge.domain.mealsession.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EsgAggregationService {

	public static final BigDecimal CO2E_FACTOR_KG_PER_KG = new BigDecimal("0.49");

	private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

	private final EsgContributionRepository esgContributionRepository;
	private final TrackingEventRepository trackingEventRepository;

	public void recordWasteReducedOnSessionClose(MealSession session) {
		BigDecimal weightKg = resolveWasteReducedKg(session);
		if (weightKg.signum() <= 0) {
			return;
		}
		recordIfAbsent(
				EsgActorType.DONOR,
				session.getDonorInstitutionId(),
				formatMonth(session),
				EsgEventType.WASTE_REDUCED,
				weightKg,
				session.getId());
	}

	public void recordSurplusDonated(UUID donorInstitutionId, UUID donationId, BigDecimal weightKg) {
		if (weightKg == null || weightKg.signum() <= 0) {
			return;
		}
		recordIfAbsent(
				EsgActorType.DONOR,
				donorInstitutionId,
				formatMonth(KstDates.now()),
				EsgEventType.SURPLUS_DONATED,
				weightKg,
				donationId);
	}

	public void recordSurplusReceived(UUID organizationId, UUID donationId, BigDecimal weightKg) {
		if (weightKg == null || weightKg.signum() <= 0) {
			return;
		}
		recordIfAbsent(
				EsgActorType.ORGANIZATION,
				organizationId,
				formatMonth(KstDates.now()),
				EsgEventType.SURPLUS_RECEIVED,
				weightKg,
				donationId);
	}

	private BigDecimal resolveWasteReducedKg(MealSession session) {
		List<TrackingEvent> servingEvents =
				trackingEventRepository.findBySessionIdAndMode(session.getId(), TrackingMode.SERVING);
		BigDecimal servedFromEvents = servingEvents.stream()
				.map(TrackingEvent::getDeltaKg)
				.filter(delta -> delta != null && delta.signum() > 0)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (servedFromEvents.signum() > 0) {
			return servedFromEvents;
		}
		if (session.getPreparedWeightKg() == null || session.getRemainingWeightKg() == null) {
			return BigDecimal.ZERO;
		}
		return session.getPreparedWeightKg()
				.subtract(session.getRemainingWeightKg())
				.max(BigDecimal.ZERO);
	}

	private void recordIfAbsent(
			EsgActorType actorType,
			UUID actorId,
			String month,
			EsgEventType eventType,
			BigDecimal weightKg,
			UUID sourceRef) {
		if (esgContributionRepository.existsByActorTypeAndActorIdAndEventTypeAndSourceRef(
				actorType, actorId, eventType, sourceRef)) {
			return;
		}
		BigDecimal co2eKg = weightKg.multiply(CO2E_FACTOR_KG_PER_KG).setScale(3, RoundingMode.HALF_UP);
		esgContributionRepository.save(EsgContribution.builder()
				.actorType(actorType)
				.actorId(actorId)
				.month(month)
				.eventType(eventType)
				.weightKg(weightKg.setScale(3, RoundingMode.HALF_UP))
				.co2eKg(co2eKg)
				.sourceRef(sourceRef)
				.recordedAt(KstDates.now())
				.build());
	}

	private String formatMonth(MealSession session) {
		return session.getSessionDate().format(MONTH_FORMAT);
	}

	private String formatMonth(java.time.LocalDateTime at) {
		return at.toLocalDate().format(MONTH_FORMAT);
	}
}
