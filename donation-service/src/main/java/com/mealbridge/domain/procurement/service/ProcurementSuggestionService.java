package com.mealbridge.domain.procurement.service;

import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.analytics.entity.MealAnalyticsDaily;
import com.mealbridge.domain.analytics.repository.MealAnalyticsDailyRepository;
import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealplan.entity.MealPlan;
import com.mealbridge.domain.mealplan.repository.MealPlanRepository;
import com.mealbridge.domain.mealsession.dto.LineItemInput;
import com.mealbridge.domain.prediction.dto.PredictedLeftoverResDto;
import com.mealbridge.domain.prediction.service.LeftoverPredictionService;
import com.mealbridge.domain.procurement.dto.MealProcurementSuggestionDto;
import com.mealbridge.domain.procurement.dto.MealProcurementSuggestionDto.LineItemProcurementSuggestionDto;
import com.mealbridge.domain.procurement.dto.MealProcurementSuggestionDto.WeatherContextDto;
import com.mealbridge.domain.procurement.dto.ProcurementSuggestionResDto;
import com.mealbridge.domain.procurement.dto.ProcurementSuggestionResDto.ProcurementDataContextDto;
import com.mealbridge.domain.weather.dto.CurrentWeatherResDto;
import com.mealbridge.domain.weather.service.WeatherService;
import com.mealbridge.domain.weather.support.WeatherLabels;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mealbridge.common.util.JsonUtil;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcurementSuggestionService {

	private static final String DISCLAIMER =
			"Suggestions combine historical serving and waste data, meal plans, live or historical weather, "
					+ "day-of-week and headcount patterns, and menu similarity scoring. "
					+ "Target waste is ~10% below similarity-weighted prediction. Verify with unit SOP before ordering.";

	private static final BigDecimal WASTE_REDUCTION_TARGET = new BigDecimal("0.90");

	private final MealAnalyticsDailyRepository mealAnalyticsDailyRepository;
	private final MealPlanRepository mealPlanRepository;
	private final LeftoverPredictionService leftoverPredictionService;
	private final WeatherService weatherService;

	public ProcurementSuggestionResDto suggest(UUID donorInstitutionId, LocalDate from, LocalDate to) {
		LocalDate today = KstDates.today();
		if (from.isBefore(today)) {
			from = today;
		}
		if (to.isBefore(from)) {
			to = from.plusDays(7);
		}

		List<MealAnalyticsDaily> history = mealAnalyticsDailyRepository
				.findByDonorInstitutionIdAndSessionDateBetweenOrderBySessionDateAsc(
						donorInstitutionId,
						today.minusYears(2),
						today.minusDays(1))
				.stream()
				.filter(row -> row.getHeadcount() != null && row.getHeadcount() > 0)
				.toList();

		CurrentWeatherResDto currentWeather = fetchWeatherSafe(donorInstitutionId);
		ProcurementDataContextDto context = buildContext(history, currentWeather);

		List<MealPlan> plans = mealPlanRepository
				.findByDonorInstitutionIdAndPlanDateBetweenOrderByPlanDateAscMealTypeAsc(
						donorInstitutionId, from, to);

		List<MealProcurementSuggestionDto> suggestions = new ArrayList<>();
		for (MealPlan plan : plans) {
			suggestions.add(suggestForPlan(plan, history, currentWeather));
		}

		if (suggestions.isEmpty() && !history.isEmpty()) {
			suggestions.addAll(suggestFromHistoryBenchmarks(donorInstitutionId, from, to, history, currentWeather));
		}

		return new ProcurementSuggestionResDto(from, to, context, DISCLAIMER, suggestions);
	}

	private MealProcurementSuggestionDto suggestForPlan(
			MealPlan plan,
			List<MealAnalyticsDaily> history,
			CurrentWeatherResDto currentWeather) {
		List<LineItemInput> lineItems = parseLineItems(plan.getLineItemsJson());
		WeatherContextDto weather = resolveWeather(plan.getPlanDate(), history, currentWeather);

		PredictedLeftoverResDto wastePrediction = leftoverPredictionService.predict(
				plan.getDonorInstitutionId(),
				plan.getPlanDate(),
				plan.getHeadcount(),
				weather.weatherCode(),
				weather.temperatureC(),
				weather.humidityPct(),
				lineItems);

		List<ScoredRow> scored = scoreSimilarSessions(
				history,
				plan.getMealType(),
				plan.getPlanDate().getDayOfWeek().getValue(),
				plan.getHeadcount(),
				weather.weatherCode(),
				weather.temperatureC(),
				weather.humidityPct(),
				menuTokens(lineItems));

		BigDecimal servedPerPerson = weightedMetric(scored, Metric.SERVED_PER_PERSON, plan.getHeadcount());
		BigDecimal predictedWaste = wastePrediction.predictedWasteKg() != null
				? wastePrediction.predictedWasteKg()
				: weightedMetric(scored, Metric.WASTE_PER_PERSON, plan.getHeadcount())
						.multiply(BigDecimal.valueOf(plan.getHeadcount()));

		BigDecimal expectedServed = servedPerPerson.multiply(BigDecimal.valueOf(plan.getHeadcount()))
				.setScale(3, RoundingMode.HALF_UP);
		BigDecimal adjustedWaste = predictedWaste.multiply(WASTE_REDUCTION_TARGET).setScale(3, RoundingMode.HALF_UP);
		BigDecimal suggestedTotal = expectedServed.add(adjustedWaste).setScale(3, RoundingMode.HALF_UP);

		if (suggestedTotal.signum() <= 0 && plan.getPreparedWeightKg().signum() > 0) {
			suggestedTotal = plan.getPreparedWeightKg()
					.multiply(new BigDecimal("0.95"))
					.setScale(3, RoundingMode.HALF_UP);
		}

		BigDecimal delta = suggestedTotal.subtract(plan.getPreparedWeightKg()).setScale(3, RoundingMode.HALF_UP);
		BigDecimal deltaPct = percentDelta(plan.getPreparedWeightKg(), suggestedTotal);

		double avgScore = scored.isEmpty()
				? 0
				: scored.stream().mapToDouble(ScoredRow::score).average().orElse(0);
		int refCount = wastePrediction.referenceSessionCount() != null
				? wastePrediction.referenceSessionCount()
				: scored.size();
		BigDecimal confidence = confidenceScore(avgScore, refCount);

		List<String> factors = buildFactors(scored, weather, wastePrediction);
		List<LineItemProcurementSuggestionDto> lineSuggestions =
				distributeLineItems(lineItems, plan.getPreparedWeightKg(), suggestedTotal);

		String rationale = buildRationale(
				plan.getPlanDate(),
				plan.getMealType(),
				refCount,
				servedPerPerson,
				predictedWaste,
				plan.getPreparedWeightKg(),
				suggestedTotal,
				deltaPct,
				weather);

		return new MealProcurementSuggestionDto(
				plan.getId(),
				plan.getPlanDate(),
				plan.getMealType(),
				plan.getHeadcount(),
				plan.getPreparedWeightKg(),
				suggestedTotal,
				delta,
				deltaPct,
				predictedWaste,
				expectedServed,
				confidence,
				refCount,
				weather,
				factors,
				lineSuggestions,
				wastePrediction.similarSessions() != null ? wastePrediction.similarSessions() : List.of(),
				rationale);
	}

	private List<MealProcurementSuggestionDto> suggestFromHistoryBenchmarks(
			UUID donorInstitutionId,
			LocalDate from,
			LocalDate to,
			List<MealAnalyticsDaily> history,
			CurrentWeatherResDto currentWeather) {
		List<MealProcurementSuggestionDto> results = new ArrayList<>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			for (MealType mealType : MealType.values()) {
				List<MealAnalyticsDaily> typeHistory = history.stream()
						.filter(row -> row.getMealType() == mealType)
						.toList();
				if (typeHistory.isEmpty()) {
					continue;
				}
				int dow = date.getDayOfWeek().getValue();
				int medianHeadcount = medianHeadcount(typeHistory);
				WeatherContextDto weather = resolveWeather(date, history, currentWeather);
				List<ScoredRow> scored = scoreSimilarSessions(
						typeHistory,
						mealType,
						dow,
						medianHeadcount,
						weather.weatherCode(),
						weather.temperatureC(),
						weather.humidityPct(),
						Set.of());

				if (scored.isEmpty()) {
					continue;
				}

				BigDecimal servedPerPerson = weightedMetric(scored, Metric.SERVED_PER_PERSON, medianHeadcount);
				BigDecimal wastePerPerson = weightedMetric(scored, Metric.WASTE_PER_PERSON, medianHeadcount);
				BigDecimal suggestedTotal = servedPerPerson
						.add(wastePerPerson.multiply(WASTE_REDUCTION_TARGET))
						.multiply(BigDecimal.valueOf(medianHeadcount))
						.setScale(3, RoundingMode.HALF_UP);

				BigDecimal predictedWaste = wastePerPerson.multiply(BigDecimal.valueOf(medianHeadcount))
						.setScale(3, RoundingMode.HALF_UP);
				BigDecimal expectedServed = servedPerPerson.multiply(BigDecimal.valueOf(medianHeadcount))
						.setScale(3, RoundingMode.HALF_UP);

				double avgScore = scored.stream().mapToDouble(ScoredRow::score).average().orElse(0);
				BigDecimal confidence = confidenceScore(avgScore, scored.size());

				String rationale = buildRationale(
						date,
						mealType,
						scored.size(),
						servedPerPerson,
						predictedWaste,
						BigDecimal.ZERO,
						suggestedTotal,
						null,
						weather);

				results.add(new MealProcurementSuggestionDto(
						null,
						date,
						mealType,
						medianHeadcount,
						BigDecimal.ZERO.setScale(3),
						suggestedTotal,
						suggestedTotal,
						null,
						predictedWaste,
						expectedServed,
						confidence,
						scored.size(),
						weather,
						buildFactors(scored, weather, null),
						List.of(),
						List.of(),
						rationale + " (No meal plan — estimated from historical patterns only)"));
			}
		}
		return results;
	}

	private ProcurementDataContextDto buildContext(
			List<MealAnalyticsDaily> history,
			CurrentWeatherResDto currentWeather) {
		if (history.isEmpty()) {
			return new ProcurementDataContextDto(
					0,
					BigDecimal.ZERO.setScale(1),
					BigDecimal.ZERO.setScale(3),
					BigDecimal.ZERO.setScale(1),
					currentWeather,
					currentWeather != null ? currentWeather.institutionName() : null);
		}

		BigDecimal totalPrepared = BigDecimal.ZERO;
		BigDecimal totalWasted = BigDecimal.ZERO;
		BigDecimal totalServed = BigDecimal.ZERO;
		int totalHeadcount = 0;

		for (MealAnalyticsDaily row : history) {
			totalPrepared = totalPrepared.add(row.getPreparedKg());
			totalWasted = totalWasted.add(row.getWastedKg());
			totalServed = totalServed.add(row.getServedKg());
			totalHeadcount += row.getHeadcount();
		}

		BigDecimal wasteRate = totalPrepared.signum() > 0
				? totalWasted.multiply(BigDecimal.valueOf(100))
						.divide(totalPrepared, 1, RoundingMode.HALF_UP)
				: BigDecimal.ZERO.setScale(1);

		BigDecimal servedPerPerson = totalHeadcount > 0
				? totalServed.divide(BigDecimal.valueOf(totalHeadcount), 3, RoundingMode.HALF_UP)
				: BigDecimal.ZERO.setScale(3);

		BigDecimal trend = computeWasteTrend(history);

		return new ProcurementDataContextDto(
				history.size(),
				wasteRate,
				servedPerPerson,
				trend,
				currentWeather,
				currentWeather != null ? currentWeather.institutionName() : null);
	}

	private BigDecimal computeWasteTrend(List<MealAnalyticsDaily> history) {
		LocalDate today = KstDates.today();
		LocalDate recentStart = today.minusDays(30);
		LocalDate priorStart = today.minusDays(60);

		BigDecimal recentWaste = sumWasteBetween(history, recentStart, today.minusDays(1));
		BigDecimal priorWaste = sumWasteBetween(history, priorStart, recentStart.minusDays(1));

		if (priorWaste.signum() <= 0) {
			return BigDecimal.ZERO.setScale(1);
		}
		return recentWaste.subtract(priorWaste)
				.multiply(BigDecimal.valueOf(100))
				.divide(priorWaste, 1, RoundingMode.HALF_UP);
	}

	private BigDecimal sumWasteBetween(List<MealAnalyticsDaily> history, LocalDate from, LocalDate to) {
		return history.stream()
				.filter(row -> !row.getSessionDate().isBefore(from) && !row.getSessionDate().isAfter(to))
				.map(MealAnalyticsDaily::getWastedKg)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private WeatherContextDto resolveWeather(
			LocalDate planDate,
			List<MealAnalyticsDaily> history,
			CurrentWeatherResDto currentWeather) {
		LocalDate today = KstDates.today();
		if (currentWeather != null && !planDate.isAfter(today.plusDays(1))) {
			return new WeatherContextDto(
					currentWeather.weatherCode(),
					currentWeather.weatherLabel(),
					currentWeather.temperatureC(),
					currentWeather.humidityPct(),
					"live");
		}

		int dow = planDate.getDayOfWeek().getValue();
		List<MealAnalyticsDaily> sameDow = history.stream()
				.filter(row -> row.getDayOfWeek() == dow)
				.filter(row -> row.getWeatherCode() != null)
				.toList();

		if (sameDow.isEmpty()) {
			return new WeatherContextDto(null, "Historical average unavailable", null, null, "historical_avg");
		}

		double avgTemp = sameDow.stream()
				.map(MealAnalyticsDaily::getTemperatureC)
				.filter(Objects::nonNull)
				.mapToDouble(Double::doubleValue)
				.average()
				.orElse(Double.NaN);
		double avgHumidity = sameDow.stream()
				.map(MealAnalyticsDaily::getHumidityPct)
				.filter(Objects::nonNull)
				.mapToInt(Integer::intValue)
				.average()
				.orElse(Double.NaN);

		Integer modeCode = sameDow.stream()
				.map(MealAnalyticsDaily::getWeatherCode)
				.filter(Objects::nonNull)
				.collect(java.util.stream.Collectors.groupingBy(c -> c, java.util.stream.Collectors.counting()))
				.entrySet()
				.stream()
				.max(java.util.Map.Entry.comparingByValue())
				.map(java.util.Map.Entry::getKey)
				.orElse(null);

		return new WeatherContextDto(
				modeCode,
				modeCode != null ? WeatherLabels.labelForCode(modeCode) : "Historical pattern",
				Double.isNaN(avgTemp) ? null : round1(avgTemp),
				Double.isNaN(avgHumidity) ? null : (int) Math.round(avgHumidity),
				"historical_avg");
	}

	private List<ScoredRow> scoreSimilarSessions(
			List<MealAnalyticsDaily> history,
			MealType mealType,
			int currentDow,
			int headcount,
			Integer weatherCode,
			Double temperatureC,
			Integer humidityPct,
			Set<String> currentMenu) {
		List<ScoredRow> scored = new ArrayList<>();
		for (MealAnalyticsDaily row : history) {
			if (row.getMealType() != mealType) {
				continue;
			}
			double score = similarityScore(row, currentDow, headcount, weatherCode, temperatureC, humidityPct, currentMenu);
			if (score <= 0.12) {
				continue;
			}
			scored.add(new ScoredRow(row, score));
		}
		scored.sort(Comparator.comparingDouble(ScoredRow::score).reversed());
		return scored.stream().limit(8).toList();
	}

	private BigDecimal weightedMetric(List<ScoredRow> scored, Metric metric, int headcount) {
		if (scored.isEmpty()) {
			return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
		}
		double weightSum = scored.stream().mapToDouble(ScoredRow::score).sum();
		if (weightSum <= 0) {
			return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
		}
		BigDecimal weighted = BigDecimal.ZERO;
		for (ScoredRow row : scored) {
			BigDecimal perPerson = metric.extract(row.row());
			BigDecimal portion = perPerson.multiply(BigDecimal.valueOf(row.score() / weightSum));
			weighted = weighted.add(portion);
		}
		if (metric == Metric.SERVED_PER_PERSON && weighted.signum() <= 0 && headcount > 0) {
			return scored.get(0).row().getServedKg()
					.divide(BigDecimal.valueOf(scored.get(0).row().getHeadcount()), 3, RoundingMode.HALF_UP);
		}
		return weighted.setScale(3, RoundingMode.HALF_UP);
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
		double menuScore = currentMenu.isEmpty() ? 0.10 : 0.30 * jaccard(currentMenu, pastMenu);

		return dayScore + weatherScore + headcountScore + menuScore;
	}

	private List<LineItemProcurementSuggestionDto> distributeLineItems(
			List<LineItemInput> lineItems,
			BigDecimal currentTotal,
			BigDecimal suggestedTotal) {
		if (lineItems == null || lineItems.isEmpty() || currentTotal.signum() <= 0) {
			return List.of();
		}
		List<LineItemProcurementSuggestionDto> result = new ArrayList<>();
		BigDecimal allocated = BigDecimal.ZERO;
		for (int i = 0; i < lineItems.size(); i++) {
			LineItemInput item = lineItems.get(i);
			BigDecimal current = item.preparedKg();
			BigDecimal suggested;
			if (i == lineItems.size() - 1) {
				suggested = suggestedTotal.subtract(allocated).setScale(3, RoundingMode.HALF_UP);
			} else {
				suggested = current
						.multiply(suggestedTotal)
						.divide(currentTotal, 3, RoundingMode.HALF_UP);
				allocated = allocated.add(suggested);
			}
			result.add(new LineItemProcurementSuggestionDto(
					item.itemName(),
					current,
					suggested,
					suggested.subtract(current).setScale(3, RoundingMode.HALF_UP)));
		}
		return result;
	}

	private List<String> buildFactors(
			List<ScoredRow> scored,
			WeatherContextDto weather,
			PredictedLeftoverResDto wastePrediction) {
		List<String> factors = new ArrayList<>();
		factors.add("Historical waste & serving records");
		factors.add("Day-of-week pattern");
		if (weather.weatherCode() != null) {
			factors.add("live".equals(weather.source()) ? "Live weather" : "Historical DOW weather");
		}
		factors.add("Headcount");
		if (!scored.isEmpty() && scored.get(0).score() > 0.25) {
			factors.add("Menu similarity");
		}
		if (wastePrediction != null && wastePrediction.referenceSessionCount() != null
				&& wastePrediction.referenceSessionCount() > 0) {
			factors.add("Similar-session waste prediction");
		}
		return factors;
	}

	private String buildRationale(
			LocalDate planDate,
			MealType mealType,
			int refCount,
			BigDecimal servedPerPerson,
			BigDecimal predictedWaste,
			BigDecimal currentPrepared,
			BigDecimal suggested,
			BigDecimal deltaPct,
			WeatherContextDto weather) {
		String mealLabel = switch (mealType) {
			case BREAKFAST -> "Breakfast";
			case LUNCH -> "Lunch";
			case DINNER -> "Dinner";
		};
		StringBuilder sb = new StringBuilder();
		sb.append(planDate).append(' ').append(mealLabel).append(": ");
		sb.append("Based on ").append(refCount).append(" similar meals, per-person serving ")
				.append(servedPerPerson).append(" kg, predicted waste ")
				.append(predictedWaste).append(" kg → suggested order ")
				.append(suggested).append(" kg");
		if (currentPrepared.signum() > 0 && deltaPct != null) {
			sb.append(" (current plan ").append(currentPrepared).append(" kg, ")
					.append(deltaPct.compareTo(BigDecimal.ZERO) > 0 ? "+" : "")
					.append(deltaPct).append("%)");
		}
		if (weather.weatherLabel() != null) {
			sb.append(". Weather: ").append(weather.weatherLabel());
			if (weather.temperatureC() != null) {
				sb.append(' ').append(weather.temperatureC()).append("°C");
			}
		}
		return sb.toString();
	}

	private BigDecimal confidenceScore(double avgSimilarity, int refCount) {
		double score = avgSimilarity * 0.55 + Math.min(refCount / 10.0, 1.0) * 0.45;
		return BigDecimal.valueOf(Math.min(1.0, score)).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal percentDelta(BigDecimal current, BigDecimal suggested) {
		if (current.signum() <= 0) {
			return null;
		}
		return suggested.subtract(current)
				.multiply(BigDecimal.valueOf(100))
				.divide(current, 1, RoundingMode.HALF_UP);
	}

	private int medianHeadcount(List<MealAnalyticsDaily> history) {
		List<Integer> counts = history.stream()
				.map(MealAnalyticsDaily::getHeadcount)
				.filter(h -> h != null && h > 0)
				.sorted()
				.toList();
		if (counts.isEmpty()) {
			return 100;
		}
		return counts.get(counts.size() / 2);
	}

	private CurrentWeatherResDto fetchWeatherSafe(UUID donorInstitutionId) {
		try {
			return weatherService.getCurrentWeather(donorInstitutionId);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	private List<LineItemInput> parseLineItems(String json) {
		List<LineItemInput> items = JsonUtil.fromJson(json, new TypeReference<>() {});
		return items != null ? items : List.of();
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

	private double round1(double value) {
		return Math.round(value * 10.0) / 10.0;
	}

	private enum Metric {
		SERVED_PER_PERSON {
			@Override
			BigDecimal extract(MealAnalyticsDaily row) {
				return row.getServedKg().divide(BigDecimal.valueOf(row.getHeadcount()), 6, RoundingMode.HALF_UP);
			}
		},
		WASTE_PER_PERSON {
			@Override
			BigDecimal extract(MealAnalyticsDaily row) {
				return row.getWastedKg().divide(BigDecimal.valueOf(row.getHeadcount()), 6, RoundingMode.HALF_UP);
			}
		};

		abstract BigDecimal extract(MealAnalyticsDaily row);
	}

	private record ScoredRow(MealAnalyticsDaily row, double score) {
	}
}
