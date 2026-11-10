package com.mealbridge.common.config;

import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealplan.entity.MealPlan;
import com.mealbridge.domain.mealplan.repository.MealPlanRepository;
import com.mealbridge.domain.mealsession.dto.LineItemInput;
import com.mealbridge.domain.mealsession.dto.LineItemSnapshot;
import com.mealbridge.domain.mealsession.entity.MealLineItem;
import com.mealbridge.domain.mealsession.entity.MealSession;
import com.mealbridge.domain.mealsession.entity.MealSessionStatus;
import com.mealbridge.domain.mealsession.entity.TrackingEvent;
import com.mealbridge.domain.mealsession.entity.TrackingMode;
import com.mealbridge.domain.mealsession.entity.TrackingSource;
import com.mealbridge.domain.mealsession.repository.MealLineItemRepository;
import com.mealbridge.domain.mealsession.repository.MealSessionRepository;
import com.mealbridge.domain.mealsession.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mealbridge.common.config.DemoSeedIds.DEMO_DONOR_INSTITUTION_ID;
import static com.mealbridge.common.config.DemoSeedIds.DEMO_DONOR_USER_ID;
import static com.mealbridge.common.config.DemoSeedIds.DEMO_SEED_MARKER;

@Slf4j
@Component
@RequiredArgsConstructor
public class DonationDemoDataInitializer implements ApplicationRunner {

	private static final String LEGACY_SEED_PREFIX = "DUMMY";

	private final MealPlanRepository mealPlanRepository;
	private final MealSessionRepository mealSessionRepository;
	private final MealLineItemRepository mealLineItemRepository;
	private final TrackingEventRepository trackingEventRepository;

	@Value("${mealbridge.demo.seed-enabled:false}")
	private boolean seedEnabled;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!seedEnabled) {
			return;
		}

		if (mealPlanRepository.existsByDonorInstitutionIdAndNotes(DEMO_DONOR_INSTITUTION_ID, DEMO_SEED_MARKER)) {
			return;
		}

		purgeLegacyDemoData();

		LocalDate today = KstDates.today();
		seedBreakfastPlanAndSession(today);
		seedPlanOnly(today, MealType.LUNCH, lunchDishes(), 128);
		seedPlanOnly(today.minusDays(1), MealType.LUNCH, simpleDishes("Chicken stew", "12.500", "Cabbage salad", "4.200"), 135);
		seedDinnerPlanAndSession(today.minusDays(1));

		for (int offset = 2; offset <= 6; offset++) {
			LocalDate date = today.minusDays(offset);
			seedPlanOnly(date, MealType.BREAKFAST, simpleDishes("Toast", "8.400", "Soup", "6.100"), 120);
			if (offset % 2 == 0) {
				seedPlanOnly(date, MealType.DINNER, simpleDishes("Pasta", "18.600", "Garlic bread", "5.400"), 130);
			}
		}

		log.info("Donation demo seed ready (marker: {})", DEMO_SEED_MARKER);
	}

	private void purgeLegacyDemoData() {
		List<MealSession> sessions = mealSessionRepository.findByDonorInstitutionIdAndNotesStartingWith(
				DEMO_DONOR_INSTITUTION_ID, LEGACY_SEED_PREFIX);
		List<UUID> sessionIds = sessions.stream().map(MealSession::getId).toList();
		if (!sessionIds.isEmpty()) {
			trackingEventRepository.deleteBySessionIdIn(sessionIds);
			mealLineItemRepository.deleteBySessionIdIn(sessionIds);
			mealSessionRepository.deleteAllInBatch(sessions);
		}
		mealPlanRepository.deleteByDonorInstitutionIdAndNotesStartingWith(
				DEMO_DONOR_INSTITUTION_ID, LEGACY_SEED_PREFIX);
	}

	private void seedBreakfastPlanAndSession(LocalDate date) {
		List<DishSeed> dishes = breakfastDishes();
		List<LineItemInput> lineItems = dishes.stream().map(DishSeed::toLineItemInput).toList();
		BigDecimal preparedTotal = sumPrepared(dishes);

		mealPlanRepository.save(MealPlan.builder()
				.donorInstitutionId(DEMO_DONOR_INSTITUTION_ID)
				.registeredByUserId(DEMO_DONOR_USER_ID)
				.mealType(MealType.BREAKFAST)
				.planDate(date)
				.preparedWeightKg(preparedTotal)
				.headcount(142)
				.lineItemsJson(JsonUtil.toJson(lineItems))
				.notes(DEMO_SEED_MARKER)
				.build());

		MealSession session = mealSessionRepository.save(MealSession.builder()
				.donorInstitutionId(DEMO_DONOR_INSTITUTION_ID)
				.registeredByUserId(DEMO_DONOR_USER_ID)
				.mealType(MealType.BREAKFAST)
				.menuName(date + " BREAKFAST — Rice, Stir-fried pork, Kimchi…")
				.sessionDate(date)
				.preparedWeightKg(preparedTotal)
				.headcount(142)
				.notes(DEMO_SEED_MARKER)
				.build());

		BigDecimal remainingTotal = BigDecimal.ZERO;
		for (DishSeed dish : dishes) {
			MealLineItem lineItem = mealLineItemRepository.save(MealLineItem.builder()
					.sessionId(session.getId())
					.itemName(dish.name())
					.preparedKg(dish.prepared())
					.perPersonKg(dish.perPerson())
					.ingredients(dish.ingredients())
					.build());
			lineItem.setRemainingKg(dish.remaining());
			remainingTotal = remainingTotal.add(dish.remaining());
		}

		session.setRemainingWeight(remainingTotal);
		session.transitionTo(MealSessionStatus.CLOSED);

		for (DishSeed dish : dishes) {
			if (dish.served().signum() > 0) {
				saveTrackingEvent(
						session.getId(),
						TrackingMode.SERVING,
						dish.served(),
						List.of(new LineItemSnapshot(dish.name(), dish.served())));
			}
			if (dish.wasted().signum() > 0) {
				saveTrackingEvent(
						session.getId(),
						TrackingMode.LEFTOVER,
						dish.wasted(),
						List.of(new LineItemSnapshot(dish.name(), dish.wasted())));
			}
		}
	}

	private void seedDinnerPlanAndSession(LocalDate date) {
		List<DishSeed> dishes = List.of(
				dish("Bulgogi", "0.210", "28.350", "24.100", "0.520", "beef, soy sauce"),
				dish("Steamed egg", "0.080", "10.800", "9.600", "0.180", "egg"),
				dish("Seasoned spinach", "0.055", "7.425", "6.900", "0.125", "spinach"));
		List<LineItemInput> lineItems = dishes.stream().map(DishSeed::toLineItemInput).toList();
		BigDecimal preparedTotal = sumPrepared(dishes);

		mealPlanRepository.save(MealPlan.builder()
				.donorInstitutionId(DEMO_DONOR_INSTITUTION_ID)
				.registeredByUserId(DEMO_DONOR_USER_ID)
				.mealType(MealType.DINNER)
				.planDate(date)
				.preparedWeightKg(preparedTotal)
				.headcount(135)
				.lineItemsJson(JsonUtil.toJson(lineItems))
				.notes(DEMO_SEED_MARKER)
				.build());

		MealSession session = mealSessionRepository.save(MealSession.builder()
				.donorInstitutionId(DEMO_DONOR_INSTITUTION_ID)
				.registeredByUserId(DEMO_DONOR_USER_ID)
				.mealType(MealType.DINNER)
				.menuName(date + " DINNER — Bulgogi, Steamed egg, Seasoned spinach")
				.sessionDate(date)
				.preparedWeightKg(preparedTotal)
				.headcount(135)
				.notes(DEMO_SEED_MARKER)
				.build());

		BigDecimal remainingTotal = BigDecimal.ZERO;
		for (DishSeed dish : dishes) {
			MealLineItem lineItem = mealLineItemRepository.save(MealLineItem.builder()
					.sessionId(session.getId())
					.itemName(dish.name())
					.preparedKg(dish.prepared())
					.perPersonKg(dish.perPerson())
					.ingredients(dish.ingredients())
					.build());
			lineItem.setRemainingKg(dish.remaining());
			remainingTotal = remainingTotal.add(dish.remaining());
		}

		session.setRemainingWeight(remainingTotal);
		session.transitionTo(MealSessionStatus.CLOSED);

		for (DishSeed dish : dishes) {
			saveTrackingEvent(
					session.getId(),
					TrackingMode.SERVING,
					dish.served(),
					List.of(new LineItemSnapshot(dish.name(), dish.served())));
			if (dish.wasted().signum() > 0) {
				saveTrackingEvent(
						session.getId(),
						TrackingMode.LEFTOVER,
						dish.wasted(),
						List.of(new LineItemSnapshot(dish.name(), dish.wasted())));
			}
		}
	}

	private void seedPlanOnly(LocalDate date, MealType mealType, List<DishSeed> dishes, int headcount) {
		List<LineItemInput> lineItems = dishes.stream().map(DishSeed::toLineItemInput).toList();
		mealPlanRepository.save(MealPlan.builder()
				.donorInstitutionId(DEMO_DONOR_INSTITUTION_ID)
				.registeredByUserId(DEMO_DONOR_USER_ID)
				.mealType(mealType)
				.planDate(date)
				.preparedWeightKg(sumPrepared(dishes))
				.headcount(headcount)
				.lineItemsJson(JsonUtil.toJson(lineItems))
				.notes(DEMO_SEED_MARKER)
				.build());
	}

	private void saveTrackingEvent(
			UUID sessionId,
			TrackingMode mode,
			BigDecimal deltaKg,
			List<LineItemSnapshot> snapshots) {
		trackingEventRepository.save(TrackingEvent.builder()
				.sessionId(sessionId)
				.mode(mode)
				.source(TrackingSource.MANUAL)
				.deltaKg(deltaKg)
				.lineItemSnapshots(JsonUtil.toJson(snapshots))
				.aiRawResponse(null)
				.build());
	}

	private static List<DishSeed> breakfastDishes() {
		return List.of(
				dish("Rice", "0.234", "33.230", "22.700", "0.413", "rice"),
				dish("Stir-fried pork", "0.284", "40.330", "29.000", "0.267", "pork, onion"),
				dish("Kimchi", "0.054", "7.670", "5.500", "0.231", "napa cabbage"),
				dish("Soybean paste soup", "0.161", "22.860", "21.900", "0.000", "soybean paste, tofu"));
	}

	private static List<DishSeed> lunchDishes() {
		return List.of(
				dish("Fried chicken", "0.195", "24.960", "0.000", "0.000", "chicken"),
				dish("Coleslaw", "0.062", "7.936", "0.000", "0.000", "cabbage"),
				dish("Rice", "0.180", "23.040", "0.000", "0.000", "rice"));
	}

	private static List<DishSeed> simpleDishes(String name1, String kg1, String name2, String kg2) {
		return List.of(
				new DishSeed(name1, null, new BigDecimal(kg1), BigDecimal.ZERO, BigDecimal.ZERO, null),
				new DishSeed(name2, null, new BigDecimal(kg2), BigDecimal.ZERO, BigDecimal.ZERO, null));
	}

	private static DishSeed dish(
			String name,
			String perPerson,
			String prepared,
			String served,
			String wasted,
			String ingredients) {
		return new DishSeed(
				name,
				new BigDecimal(perPerson),
				new BigDecimal(prepared),
				new BigDecimal(served),
				new BigDecimal(wasted),
				ingredients);
	}

	private static BigDecimal sumPrepared(List<DishSeed> dishes) {
		return dishes.stream()
				.map(DishSeed::prepared)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private record DishSeed(
			String name,
			BigDecimal perPerson,
			BigDecimal prepared,
			BigDecimal served,
			BigDecimal wasted,
			String ingredients) {

		BigDecimal remaining() {
			return prepared.subtract(served).subtract(wasted).max(BigDecimal.ZERO);
		}

		LineItemInput toLineItemInput() {
			return new LineItemInput(name, prepared, perPerson, ingredients, null, null);
		}
	}
}
