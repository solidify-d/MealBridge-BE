package com.mealbridge.domain.mealsession.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealsession.dto.LineItemSnapshot;
import com.mealbridge.domain.mealsession.dto.MealItemBreakdown;
import com.mealbridge.domain.mealsession.dto.MealSessionBreakdown;
import com.mealbridge.domain.mealsession.dto.MealSlotSummary;
import com.mealbridge.domain.mealsession.dto.MealStatisticsResDto;
import com.mealbridge.domain.mealsession.dto.MealTrackingEventDetail;
import com.mealbridge.domain.mealsession.dto.MealTypeDailyPoint;
import com.mealbridge.domain.mealsession.dto.MealTypeHourlyPoint;
import com.mealbridge.domain.mealsession.entity.MealLineItem;
import com.mealbridge.domain.mealsession.entity.MealSession;
import com.mealbridge.domain.mealsession.entity.TrackingEvent;
import com.mealbridge.domain.mealsession.entity.TrackingMode;
import com.mealbridge.domain.mealsession.repository.MealLineItemRepository;
import com.mealbridge.domain.mealsession.repository.MealSessionRepository;
import com.mealbridge.domain.mealsession.repository.TrackingEventRepository;
import com.mealbridge.domain.weather.dto.SessionWeatherResDto;
import com.mealbridge.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealStatisticsServiceImpl implements MealStatisticsService {

	private static final int MAX_EVENTS_PER_MEAL_SLOT = 150;
	private static final String UNSPECIFIED_ITEM = "Unspecified";

	private final TrackingEventRepository trackingEventRepository;
	private final MealSessionRepository mealSessionRepository;
	private final MealLineItemRepository mealLineItemRepository;
	private final WeatherService weatherService;

	@Override
	public MealStatisticsResDto mealStatistics(
			UUID actorUserId,
			UUID donorInstitutionId,
			LocalDate from,
			LocalDate to) {
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("'from' must be on or before 'to'");
		}

		List<MealSession> sessionsInRange = mealSessionRepository
				.findByDonorInstitutionIdAndSessionDateBetweenOrderBySessionDateAscMealTypeAsc(
						donorInstitutionId, from, to);

		Map<UUID, MealSession> sessionsById = sessionsInRange.stream()
				.collect(Collectors.toMap(MealSession::getId, s -> s));

		List<TrackingEvent> events = trackingEventRepository.findAllForStatistics(
				donorInstitutionId, from, to);

		java.util.Set<UUID> missingSessionIds = events.stream()
				.map(TrackingEvent::getSessionId)
				.filter(id -> !sessionsById.containsKey(id))
				.collect(Collectors.toSet());
		if (!missingSessionIds.isEmpty()) {
			mealSessionRepository.findAllById(missingSessionIds)
					.forEach(s -> sessionsById.put(s.getId(), s));
		}

		EnumMap<MealType, BigDecimal> preparedByMeal = new EnumMap<>(MealType.class);
		for (MealSession session : sessionsInRange) {
			preparedByMeal.merge(session.getMealType(), session.getPreparedWeightKg(), BigDecimal::add);
		}

		Map<LocalDate, EnumMap<MealType, BigDecimal>> servingDaily = new HashMap<>();
		Map<LocalDate, EnumMap<MealType, BigDecimal>> leftoverDaily = new HashMap<>();
		Map<Integer, EnumMap<MealType, BigDecimal>> servingHourly = new HashMap<>();
		Map<Integer, EnumMap<MealType, BigDecimal>> leftoverHourly = new HashMap<>();

		boolean singleDay = from.equals(to);

		for (TrackingEvent event : events) {
			MealSession session = sessionsById.get(event.getSessionId());
			if (session == null) {
				continue;
			}
			BigDecimal kg = effectiveKg(event);
			if (kg.signum() <= 0) {
				continue;
			}

			LocalDate day = session.getSessionDate();
			MealType mealType = session.getMealType();
			Map<LocalDate, EnumMap<MealType, BigDecimal>> dailyTarget =
					event.getMode() == TrackingMode.SERVING ? servingDaily : leftoverDaily;
			dailyTarget
					.computeIfAbsent(day, d -> new EnumMap<>(MealType.class))
					.merge(mealType, kg, BigDecimal::add);

			if (singleDay && day.equals(from)) {
				int hour = event.getCreatedAt().getHour();
				Map<Integer, EnumMap<MealType, BigDecimal>> hourlyTarget =
						event.getMode() == TrackingMode.SERVING ? servingHourly : leftoverHourly;
				hourlyTarget
						.computeIfAbsent(hour, h -> new EnumMap<>(MealType.class))
						.merge(mealType, kg, BigDecimal::add);
			}
		}

		List<MealTypeDailyPoint> servingDailyPoints = buildDailySeries(from, to, servingDaily);
		List<MealTypeDailyPoint> leftoverDailyPoints = buildDailySeries(from, to, leftoverDaily);
		List<MealTypeHourlyPoint> servingHourlyPoints = singleDay
				? buildHourlySeries(servingHourly)
				: List.of();
		List<MealTypeHourlyPoint> leftoverHourlyPoints = singleDay
				? buildHourlySeries(leftoverHourly)
				: List.of();

		List<UUID> sessionIds = sessionsInRange.stream().map(MealSession::getId).toList();
		Map<UUID, List<MealLineItem>> lineItemsBySession = sessionIds.isEmpty()
				? Map.of()
				: mealLineItemRepository.findBySessionIdInOrderBySessionIdAscCreatedAtAsc(sessionIds)
						.stream()
						.collect(Collectors.groupingBy(MealLineItem::getSessionId));

		List<MealSlotSummary> mealSummaries = buildMealSummaries(
				sessionsInRange,
				sessionsById,
				events,
				lineItemsBySession,
				preparedByMeal,
				servingDailyPoints,
				leftoverDailyPoints);

		return new MealStatisticsResDto(
				from,
				to,
				singleDay,
				servingDailyPoints,
				leftoverDailyPoints,
				servingHourlyPoints,
				leftoverHourlyPoints,
				mealSummaries);
	}

	private List<MealSlotSummary> buildMealSummaries(
			List<MealSession> sessionsInRange,
			Map<UUID, MealSession> sessionsById,
			List<TrackingEvent> events,
			Map<UUID, List<MealLineItem>> lineItemsBySession,
			EnumMap<MealType, BigDecimal> preparedByMeal,
			List<MealTypeDailyPoint> servingDaily,
			List<MealTypeDailyPoint> leftoverDaily) {
		EnumMap<MealType, MealSlotDetailBuilder> builders = new EnumMap<>(MealType.class);
		for (MealType mealType : MealType.values()) {
			builders.put(mealType, new MealSlotDetailBuilder());
		}

		for (MealSession session : sessionsInRange) {
			MealSlotDetailBuilder builder = builders.get(session.getMealType());
			builder.registerSession(session, lineItemsBySession.getOrDefault(session.getId(), List.of()));
		}

		List<TrackingEvent> sortedEvents = events.stream()
				.sorted(Comparator.comparing(TrackingEvent::getCreatedAt).reversed())
				.toList();
		for (TrackingEvent event : sortedEvents) {
			MealSession session = sessionsById.get(event.getSessionId());
			if (session == null) {
				continue;
			}
			BigDecimal totalKg = effectiveKg(event);
			if (totalKg.signum() <= 0) {
				continue;
			}
			List<LineItemSnapshot> snapshots = parseSnapshots(event);
			builders.get(session.getMealType())
					.recordEvent(session, event.getCreatedAt(), event.getMode(), totalKg, snapshots);
		}

		return List.of(
				builders.get(MealType.BREAKFAST).toSummary(
						MealType.BREAKFAST, preparedByMeal, servingDaily, leftoverDaily, weatherService),
				builders.get(MealType.LUNCH).toSummary(
						MealType.LUNCH, preparedByMeal, servingDaily, leftoverDaily, weatherService),
				builders.get(MealType.DINNER).toSummary(
						MealType.DINNER, preparedByMeal, servingDaily, leftoverDaily, weatherService));
	}

	private static final class ItemAccumulator {
		BigDecimal prepared = BigDecimal.ZERO;
		BigDecimal served = BigDecimal.ZERO;
		BigDecimal wasted = BigDecimal.ZERO;
		String ingredients;

		void addPrepared(BigDecimal kg, String ingredientsText) {
			if (kg != null && kg.signum() > 0) {
				prepared = prepared.add(kg);
			}
			if (ingredients == null && ingredientsText != null && !ingredientsText.isBlank()) {
				ingredients = ingredientsText.trim();
			}
		}

		void addServed(BigDecimal kg) {
			if (kg != null && kg.signum() > 0) {
				served = served.add(kg);
			}
		}

		void addWasted(BigDecimal kg) {
			if (kg != null && kg.signum() > 0) {
				wasted = wasted.add(kg);
			}
		}

		MealItemBreakdown toBreakdown(String displayName) {
			BigDecimal wastePercent = BigDecimal.ZERO;
			if (prepared.signum() > 0) {
				wastePercent = wasted
						.multiply(BigDecimal.valueOf(100))
						.divide(prepared, 1, RoundingMode.HALF_UP);
			}
			return new MealItemBreakdown(
					displayName, prepared, served, wasted, wastePercent, ingredients);
		}
	}

	private static final class SessionDetailBuilder {
		private final MealSession session;
		private final Map<String, ItemAccumulator> items = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		private final Map<String, String> displayNames = new HashMap<>();

		SessionDetailBuilder(MealSession session) {
			this.session = session;
		}

		void addLineItem(MealLineItem lineItem) {
			String key = normalizeItemKey(lineItem.getItemName());
			displayNames.putIfAbsent(key, lineItem.getItemName().trim());
			items.computeIfAbsent(key, k -> new ItemAccumulator())
					.addPrepared(lineItem.getPreparedKg(), lineItem.getIngredients());
		}

		void applyEvent(TrackingMode mode, List<LineItemSnapshot> snapshots, BigDecimal totalKg) {
			applySnapshotsToItems(items, displayNames, mode, snapshots, totalKg);
		}

		MealSessionBreakdown toBreakdown(WeatherService weatherService) {
			List<MealItemBreakdown> itemRows = items.entrySet().stream()
					.map(e -> e.getValue().toBreakdown(displayNames.getOrDefault(e.getKey(), e.getKey())))
					.sorted(Comparator.comparing(MealItemBreakdown::wastedKg).reversed()
							.thenComparing(MealItemBreakdown::itemName))
					.toList();
			BigDecimal prepared = itemRows.stream()
					.map(MealItemBreakdown::preparedKg)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal served = itemRows.stream()
					.map(MealItemBreakdown::servedKg)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal wasted = itemRows.stream()
					.map(MealItemBreakdown::wastedKg)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			return new MealSessionBreakdown(
					session.getSessionDate(),
					session.getMenuName(),
					session.getHeadcount(),
					prepared,
					served,
					wasted,
					itemRows,
					weatherService.toSessionWeatherDto(session));
		}
	}

	private static final class MealSlotDetailBuilder {
		private final Map<String, ItemAccumulator> items = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		private final Map<String, String> displayNames = new HashMap<>();
		private final Map<UUID, SessionDetailBuilder> sessions = new LinkedHashMap<>();
		private final List<MealTrackingEventDetail> events = new ArrayList<>();

		void registerSession(MealSession session, List<MealLineItem> lineItems) {
			SessionDetailBuilder sessionBuilder = new SessionDetailBuilder(session);
			for (MealLineItem lineItem : lineItems) {
				String key = normalizeItemKey(lineItem.getItemName());
				displayNames.putIfAbsent(key, lineItem.getItemName().trim());
				items.computeIfAbsent(key, k -> new ItemAccumulator())
						.addPrepared(lineItem.getPreparedKg(), lineItem.getIngredients());
				sessionBuilder.addLineItem(lineItem);
			}
			sessions.put(session.getId(), sessionBuilder);
		}

		void recordEvent(
				MealSession session,
				LocalDateTime occurredAt,
				TrackingMode mode,
				BigDecimal totalKg,
				List<LineItemSnapshot> snapshots) {
			SessionDetailBuilder sessionBuilder = sessions.computeIfAbsent(
					session.getId(), id -> new SessionDetailBuilder(session));
			applySnapshotsToItems(items, displayNames, mode, snapshots, totalKg);
			sessionBuilder.applyEvent(mode, snapshots, totalKg);
			if (events.size() < MAX_EVENTS_PER_MEAL_SLOT) {
				events.add(new MealTrackingEventDetail(
						occurredAt,
						session.getMenuName(),
						mode,
						totalKg,
						snapshots.isEmpty()
								? List.of(new LineItemSnapshot(UNSPECIFIED_ITEM, totalKg))
								: snapshots));
			}
		}

		MealSlotSummary toSummary(
				MealType mealType,
				EnumMap<MealType, BigDecimal> preparedByMeal,
				List<MealTypeDailyPoint> servingDaily,
				List<MealTypeDailyPoint> leftoverDaily,
				WeatherService weatherService) {
			BigDecimal prepared = preparedByMeal.getOrDefault(mealType, BigDecimal.ZERO);
			BigDecimal served = sumDailyByMeal(servingDaily, mealType);
			BigDecimal wasted = sumDailyByMeal(leftoverDaily, mealType);
			BigDecimal wastePercent = BigDecimal.ZERO;
			if (prepared.signum() > 0) {
				wastePercent = wasted
						.multiply(BigDecimal.valueOf(100))
						.divide(prepared, 1, RoundingMode.HALF_UP);
			}
			List<MealItemBreakdown> itemRows = items.entrySet().stream()
					.map(e -> e.getValue().toBreakdown(displayNames.getOrDefault(e.getKey(), e.getKey())))
					.sorted(Comparator.comparing(MealItemBreakdown::wastedKg).reversed()
							.thenComparing(MealItemBreakdown::itemName))
					.toList();
			List<MealSessionBreakdown> sessionRows = sessions.values().stream()
					.map(builder -> builder.toBreakdown(weatherService))
					.sorted(Comparator.comparing(MealSessionBreakdown::sessionDate).reversed())
					.toList();
			return new MealSlotSummary(
					mealType,
					prepared,
					wasted,
					served,
					wastePercent,
					sessionRows.size(),
					itemRows,
					sessionRows,
					List.copyOf(events));
		}
	}

	private static void applySnapshotsToItems(
			Map<String, ItemAccumulator> items,
			Map<String, String> displayNames,
			TrackingMode mode,
			List<LineItemSnapshot> snapshots,
			BigDecimal totalKg) {
		if (snapshots == null || snapshots.isEmpty()) {
			String key = normalizeItemKey(UNSPECIFIED_ITEM);
			displayNames.putIfAbsent(key, UNSPECIFIED_ITEM);
			ItemAccumulator acc = items.computeIfAbsent(key, k -> new ItemAccumulator());
			if (mode == TrackingMode.SERVING) {
				acc.addServed(totalKg);
			} else {
				acc.addWasted(totalKg);
			}
			return;
		}
		for (LineItemSnapshot snapshot : snapshots) {
			if (snapshot.kg() == null || snapshot.kg().signum() <= 0) {
				continue;
			}
			String key = normalizeItemKey(snapshot.itemName());
			displayNames.putIfAbsent(key, snapshot.itemName().trim());
			ItemAccumulator acc = items.computeIfAbsent(key, k -> new ItemAccumulator());
			if (mode == TrackingMode.SERVING) {
				acc.addServed(snapshot.kg());
			} else {
				acc.addWasted(snapshot.kg());
			}
		}
	}

	private static String normalizeItemKey(String itemName) {
		if (itemName == null || itemName.isBlank()) {
			return UNSPECIFIED_ITEM.toLowerCase();
		}
		return itemName.trim().toLowerCase();
	}

	private static BigDecimal sumDailyByMeal(List<MealTypeDailyPoint> points, MealType mealType) {
		return points.stream()
				.map(p -> kgForMeal(p, mealType))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal kgForMeal(MealTypeDailyPoint point, MealType mealType) {
		return switch (mealType) {
			case BREAKFAST -> point.breakfastKg();
			case LUNCH -> point.lunchKg();
			case DINNER -> point.dinnerKg();
		};
	}

	private List<MealTypeDailyPoint> buildDailySeries(
			LocalDate from,
			LocalDate to,
			Map<LocalDate, EnumMap<MealType, BigDecimal>> buckets) {
		return from.datesUntil(to.plusDays(1))
				.map(date -> toDailyPoint(date, buckets.get(date)))
				.toList();
	}

	private MealTypeDailyPoint toDailyPoint(LocalDate date, EnumMap<MealType, BigDecimal> bucket) {
		return new MealTypeDailyPoint(
				date,
				kgFor(bucket, MealType.BREAKFAST),
				kgFor(bucket, MealType.LUNCH),
				kgFor(bucket, MealType.DINNER));
	}

	private List<MealTypeHourlyPoint> buildHourlySeries(Map<Integer, EnumMap<MealType, BigDecimal>> buckets) {
		return java.util.stream.IntStream.range(0, 24)
				.mapToObj(hour -> new MealTypeHourlyPoint(
						hour,
						kgFor(buckets.get(hour), MealType.BREAKFAST),
						kgFor(buckets.get(hour), MealType.LUNCH),
						kgFor(buckets.get(hour), MealType.DINNER)))
				.toList();
	}

	private BigDecimal kgFor(EnumMap<MealType, BigDecimal> bucket, MealType type) {
		if (bucket == null) {
			return BigDecimal.ZERO;
		}
		return bucket.getOrDefault(type, BigDecimal.ZERO);
	}

	private BigDecimal effectiveKg(TrackingEvent event) {
		if (event.getDeltaKg() != null && event.getDeltaKg().signum() > 0) {
			return event.getDeltaKg();
		}
		List<LineItemSnapshot> snapshots = parseSnapshots(event);
		return snapshots.stream()
				.map(LineItemSnapshot::kg)
				.filter(kg -> kg != null && kg.signum() > 0)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private List<LineItemSnapshot> parseSnapshots(TrackingEvent event) {
		List<LineItemSnapshot> snapshots = JsonUtil.fromJson(
				event.getLineItemSnapshots(),
				new TypeReference<List<LineItemSnapshot>>() {});
		return snapshots != null ? snapshots : List.of();
	}
}
