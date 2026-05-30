package com.mealbridge.domain.prediction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.domain.analytics.entity.MealAnalyticsDaily;
import com.mealbridge.domain.analytics.repository.MealAnalyticsDailyRepository;
import com.mealbridge.domain.mealsession.dto.LineItemInput;
import com.mealbridge.domain.mealsession.entity.MealSession;
import com.mealbridge.domain.mealsession.entity.MealSessionStatus;
import com.mealbridge.domain.mealsession.repository.MealSessionRepository;
import com.mealbridge.domain.prediction.dto.PredictedLeftoverResDto;
import com.mealbridge.domain.prediction.dto.PredictedLeftoverResDto.SimilarSessionRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeftoverPredictionService {

	private static final String DISCLAIMER =
			"Based on similar past meals (weather, day, headcount, menu). Actual results may differ.";

	private final MealAnalyticsDailyRepository mealAnalyticsDailyRepository;
	private final MealSessionRepository mealSessionRepository;

	public PredictedLeftoverResDto predict(
			UUID donorInstitutionId,
			LocalDate sessionDate,
			Integer headcount,
			Integer weatherCode,
			Double temperatureC,
			Integer humidityPct,
			List<LineItemInput> lineItems) {
		if (headcount == null || headcount <= 0) {
			return emptyPrediction();
		}

		Set<String> currentMenu = menuTokens(lineItems);
		int currentDow = sessionDate.getDayOfWeek().getValue();

		List<MealAnalyticsDaily> history = mealAnalyticsDailyRepository
				.findByDonorInstitutionIdAndSessionDateBetweenOrderBySessionDateAsc(
						donorInstitutionId,
						sessionDate.minusYears(2),
						sessionDate.minusDays(1))
				.stream()
				.filter(row -> row.getWastedKg().signum() > 0)
				.filter(row -> row.getHeadcount() != null && row.getHeadcount() > 0)
				.toList();

		if (history.isEmpty()) {
			return emptyPrediction();
		}

		List<ScoredRow> scored = new ArrayList<>();
		for (MealAnalyticsDaily row : history) {
			double score = similarityScore(row, currentDow, headcount, weatherCode, temperatureC, humidityPct, currentMenu);
			if (score <= 0.15) {
				continue;
			}
			BigDecimal wastePerPerson = row.getWastedKg()
					.divide(BigDecimal.valueOf(row.getHeadcount()), 6, RoundingMode.HALF_UP);
			scored.add(new ScoredRow(row, score, wastePerPerson));
		}

		if (scored.isEmpty()) {
			return emptyPrediction();
		}

		scored.sort(Comparator.comparingDouble(ScoredRow::score).reversed());
		List<ScoredRow> top = scored.stream().limit(5).toList();

		double weightSum = top.stream().mapToDouble(ScoredRow::score).sum();
		BigDecimal weightedPerPerson = BigDecimal.ZERO;
		for (ScoredRow row : top) {
			BigDecimal portion = row.wastePerPerson()
					.multiply(BigDecimal.valueOf(row.score() / weightSum));
			weightedPerPerson = weightedPerPerson.add(portion);
		}

		BigDecimal predicted = weightedPerPerson
				.multiply(BigDecimal.valueOf(headcount))
				.setScale(3, RoundingMode.HALF_UP);

		List<SimilarSessionRef> refs = top.stream()
				.map(r -> new SimilarSessionRef(
						r.row().getSessionId(),
						r.row().getSessionDate().toString(),
						round(r.score()),
						r.wastePerPerson().setScale(3, RoundingMode.HALF_UP)))
				.toList();

		return new PredictedLeftoverResDto(
				predicted,
				weightedPerPerson.setScale(3, RoundingMode.HALF_UP),
				top.size(),
				DISCLAIMER,
				refs);
	}

	public void attachPrediction(MealSession session, List<LineItemInput> lineItems) {
		PredictedLeftoverResDto prediction = predict(
				session.getDonorInstitutionId(),
				session.getSessionDate(),
				session.getHeadcount(),
				session.getWeatherCode(),
				session.getWeatherTemperatureC(),
				session.getWeatherHumidityPct(),
				lineItems);
		session.setPredictedLeftoverJson(JsonUtil.toJson(prediction));
	}

	public PredictedLeftoverResDto readFromSession(MealSession session) {
		if (session.getPredictedLeftoverJson() == null || session.getPredictedLeftoverJson().isBlank()) {
			return null;
		}
		return JsonUtil.fromJson(session.getPredictedLeftoverJson(), new TypeReference<PredictedLeftoverResDto>() {});
	}

	private PredictedLeftoverResDto emptyPrediction() {
		return new PredictedLeftoverResDto(null, null, 0, DISCLAIMER, List.of());
	}

	private double similarityScore(
			MealAnalyticsDaily row,
			int currentDow,
			int headcount,
			Integer weatherCode,
			Double temperatureC,
			Integer humidityPct,
			Set<String> currentMenu) {
		double dayScore = row.getDayOfWeek() == currentDow ? 0.20 : 0.0;

		double weatherScore = 0.0;
		if (weatherCode != null && weatherCode.equals(row.getWeatherCode())) {
			weatherScore += 0.10;
		}
		if (temperatureC != null && row.getTemperatureC() != null) {
			double delta = Math.abs(temperatureC - row.getTemperatureC());
			weatherScore += 0.075 * Math.max(0, 1 - delta / 15.0);
		}
		if (humidityPct != null && row.getHumidityPct() != null) {
			double delta = Math.abs(humidityPct - row.getHumidityPct());
			weatherScore += 0.075 * Math.max(0, 1 - delta / 40.0);
		}

		double headcountScore = 0.25 * Math.max(
				0,
				1 - Math.abs(headcount - row.getHeadcount()) / (double) Math.max(headcount, row.getHeadcount()));

		Set<String> pastMenu = menuTokensFromFingerprint(row.getMenuFingerprint());
		double menuScore = 0.30 * jaccard(currentMenu, pastMenu);

		return dayScore + weatherScore + headcountScore + menuScore;
	}

	private Set<String> menuTokens(List<LineItemInput> lineItems) {
		Set<String> tokens = new HashSet<>();
		if (lineItems == null) {
			return tokens;
		}
		for (LineItemInput item : lineItems) {
			if (item.itemName() != null) {
				tokens.add(item.itemName().trim().toLowerCase(Locale.ROOT));
			}
		}
		return tokens;
	}

	private Set<String> menuTokensFromFingerprint(String fingerprint) {
		Set<String> tokens = new HashSet<>();
		if (fingerprint == null) {
			return tokens;
		}
		for (String part : fingerprint.split("\\|")) {
			if (!part.isBlank()) {
				tokens.add(part.trim().toLowerCase(Locale.ROOT));
			}
		}
		return tokens;
	}

	private double jaccard(Set<String> a, Set<String> b) {
		if (a.isEmpty() && b.isEmpty()) {
			return 0;
		}
		Set<String> union = new HashSet<>(a);
		union.addAll(b);
		Set<String> intersection = new HashSet<>(a);
		intersection.retainAll(b);
		return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
	}

	private double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

	private record ScoredRow(MealAnalyticsDaily row, double score, BigDecimal wastePerPerson) {
	}
}
