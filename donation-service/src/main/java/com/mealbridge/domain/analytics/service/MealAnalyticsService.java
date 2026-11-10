package com.mealbridge.domain.analytics.service;

import com.mealbridge.domain.analytics.dto.MealAnalyticsGroupResDto;
import com.mealbridge.domain.analytics.dto.MealAnalyticsResDto;
import com.mealbridge.domain.analytics.entity.MealAnalyticsDaily;
import com.mealbridge.domain.analytics.repository.MealAnalyticsDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealAnalyticsService {

	private final MealAnalyticsDailyRepository mealAnalyticsDailyRepository;

	public MealAnalyticsResDto analytics(UUID donorInstitutionId, LocalDate from, LocalDate to, String groupBy) {
		List<MealAnalyticsDaily> rows = mealAnalyticsDailyRepository
				.findByDonorInstitutionIdAndSessionDateBetweenOrderBySessionDateAsc(donorInstitutionId, from, to);
		String normalized = groupBy == null ? "day" : groupBy.toLowerCase(Locale.ROOT);
		Map<String, Aggregator> buckets = new LinkedHashMap<>();
		for (MealAnalyticsDaily row : rows) {
			String key = switch (normalized) {
				case "menu" -> truncate(row.getMenuFingerprint(), 40);
				case "dow" -> dayLabel(row.getDayOfWeek());
				default -> row.getSessionDate().toString();
			};
			buckets.computeIfAbsent(key, k -> new Aggregator(key, labelFor(normalized, key, row)))
					.add(row);
		}
		List<MealAnalyticsGroupResDto> groups = buckets.values().stream()
				.map(Aggregator::toDto)
				.sorted(Comparator.comparing(MealAnalyticsGroupResDto::groupKey))
				.toList();
		return new MealAnalyticsResDto(normalized, groups);
	}

	private String labelFor(String groupBy, String key, MealAnalyticsDaily row) {
		return switch (groupBy) {
			case "menu" -> key;
			case "dow" -> key;
			default -> key;
		};
	}

	private String dayLabel(int dayOfWeek) {
		return DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return "Unknown";
		}
		return value.length() <= max ? value : value.substring(0, max - 1) + "…";
	}

	private static final class Aggregator {
		private final String groupKey;
		private final String groupLabel;
		private int sessionCount;
		private BigDecimal prepared = BigDecimal.ZERO;
		private BigDecimal served = BigDecimal.ZERO;
		private BigDecimal wasted = BigDecimal.ZERO;
		private double tempSum;
		private int tempCount;
		private int humiditySum;
		private int humidityCount;

		Aggregator(String groupKey, String groupLabel) {
			this.groupKey = groupKey;
			this.groupLabel = groupLabel;
		}

		void add(MealAnalyticsDaily row) {
			sessionCount++;
			prepared = prepared.add(row.getPreparedKg());
			served = served.add(row.getServedKg());
			wasted = wasted.add(row.getWastedKg());
			if (row.getTemperatureC() != null) {
				tempSum += row.getTemperatureC();
				tempCount++;
			}
			if (row.getHumidityPct() != null) {
				humiditySum += row.getHumidityPct();
				humidityCount++;
			}
		}

		MealAnalyticsGroupResDto toDto() {
			BigDecimal wastePercent = prepared.signum() > 0
					? wasted.multiply(BigDecimal.valueOf(100)).divide(prepared, 1, RoundingMode.HALF_UP)
					: BigDecimal.ZERO;
			return new MealAnalyticsGroupResDto(
					groupKey,
					groupLabel,
					sessionCount,
					prepared.setScale(3, RoundingMode.HALF_UP),
					served.setScale(3, RoundingMode.HALF_UP),
					wasted.setScale(3, RoundingMode.HALF_UP),
					wastePercent,
					tempCount > 0 ? tempSum / tempCount : null,
					humidityCount > 0 ? humiditySum / humidityCount : null);
		}
	}
}
