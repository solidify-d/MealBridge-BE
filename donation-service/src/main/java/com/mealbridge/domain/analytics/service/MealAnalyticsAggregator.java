package com.mealbridge.domain.analytics.service;

import com.mealbridge.domain.analytics.entity.MealAnalyticsDaily;
import com.mealbridge.domain.analytics.repository.MealAnalyticsDailyRepository;
import com.mealbridge.domain.mealsession.entity.MealLineItem;
import com.mealbridge.domain.mealsession.entity.MealSession;
import com.mealbridge.domain.mealsession.entity.MealSessionStatus;
import com.mealbridge.domain.mealsession.entity.TrackingEvent;
import com.mealbridge.domain.mealsession.entity.TrackingMode;
import com.mealbridge.domain.mealsession.repository.MealLineItemRepository;
import com.mealbridge.domain.mealsession.repository.MealSessionRepository;
import com.mealbridge.domain.mealsession.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealAnalyticsAggregator {

	private final MealSessionRepository mealSessionRepository;
	private final MealLineItemRepository mealLineItemRepository;
	private final TrackingEventRepository trackingEventRepository;
	private final MealAnalyticsDailyRepository mealAnalyticsDailyRepository;

	@Transactional
	public void upsertFromSession(MealSession session) {
		if (session.getStatus() != MealSessionStatus.CLOSED) {
			return;
		}

		List<TrackingEvent> events = trackingEventRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
		BigDecimal servedKg = events.stream()
				.filter(e -> e.getMode() == TrackingMode.SERVING)
				.map(TrackingEvent::getDeltaKg)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal wastedKg = events.stream()
				.filter(e -> e.getMode() == TrackingMode.LEFTOVER)
				.map(TrackingEvent::getDeltaKg)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (wastedKg.signum() == 0 && session.getRemainingWeightKg() != null) {
			wastedKg = session.getRemainingWeightKg();
		}
		if (servedKg.signum() == 0) {
			servedKg = session.getPreparedWeightKg().subtract(wastedKg).max(BigDecimal.ZERO);
		}

		List<MealLineItem> lineItems = mealLineItemRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
		String menuFingerprint = lineItems.stream()
				.map(MealLineItem::getItemName)
				.map(String::trim)
				.map(String::toLowerCase)
				.sorted()
				.collect(Collectors.joining("|"));

		DayOfWeek dow = session.getSessionDate().getDayOfWeek();
		MealAnalyticsDaily row = mealAnalyticsDailyRepository.findBySessionId(session.getId()).orElse(null);
		if (row == null) {
			mealAnalyticsDailyRepository.save(MealAnalyticsDaily.builder()
					.donorInstitutionId(session.getDonorInstitutionId())
					.sessionId(session.getId())
					.sessionDate(session.getSessionDate())
					.dayOfWeek(dow.getValue())
					.mealType(session.getMealType())
					.menuFingerprint(menuFingerprint.isBlank() ? session.getMenuName() : menuFingerprint)
					.headcount(session.getHeadcount())
					.preparedKg(session.getPreparedWeightKg())
					.servedKg(servedKg)
					.wastedKg(wastedKg)
					.weatherCode(session.getWeatherCode())
					.temperatureC(session.getWeatherTemperatureC())
					.humidityPct(session.getWeatherHumidityPct())
					.build());
		} else {
			row.updateMetrics(
					session.getHeadcount(),
					session.getPreparedWeightKg(),
					servedKg,
					wastedKg,
					session.getWeatherCode(),
					session.getWeatherTemperatureC(),
					session.getWeatherHumidityPct(),
					menuFingerprint.isBlank() ? session.getMenuName() : menuFingerprint);
		}
	}

	@Scheduled(fixedDelay = 300_000)
	@Transactional
	public void backfillClosedSessions() {
		List<MealSession> closed = mealSessionRepository.findByStatus(MealSessionStatus.CLOSED);
		for (MealSession session : closed) {
			if (mealAnalyticsDailyRepository.findBySessionId(session.getId()).isEmpty()) {
				try {
					upsertFromSession(session);
				} catch (RuntimeException ex) {
					log.warn("Failed analytics backfill for session {}: {}", session.getId(), ex.getMessage());
				}
			}
		}
	}
}
