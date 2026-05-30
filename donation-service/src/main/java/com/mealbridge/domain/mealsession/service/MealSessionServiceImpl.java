package com.mealbridge.domain.mealsession.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.domain.mealsession.dto.ConfirmTrackingEventReq;
import com.mealbridge.domain.mealsession.dto.CreateMealSessionReq;
import com.mealbridge.domain.mealsession.dto.LineItemInput;
import com.mealbridge.domain.mealsession.dto.LineItemSnapshot;
import com.mealbridge.domain.mealsession.dto.MealLineItemResDto;
import com.mealbridge.domain.mealsession.dto.MealSessionResDto;
import com.mealbridge.domain.mealsession.dto.TrackingEventResDto;
import com.mealbridge.domain.mealsession.entity.MealLineItem;
import com.mealbridge.domain.mealsession.entity.MealSession;
import com.mealbridge.domain.mealsession.entity.MealSessionStatus;
import com.mealbridge.domain.mealsession.entity.TrackingEvent;
import com.mealbridge.domain.mealsession.entity.TrackingMode;
import com.mealbridge.domain.mealplan.entity.MealPlan;
import com.mealbridge.domain.mealplan.repository.MealPlanRepository;
import com.mealbridge.domain.mealsession.support.TrackingEventValidation;
import com.mealbridge.domain.mealsession.repository.MealLineItemRepository;
import com.mealbridge.domain.mealsession.repository.MealSessionRepository;
import com.mealbridge.domain.mealsession.repository.TrackingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MealSessionServiceImpl implements MealSessionService {

	private static final EnumSet<MealSessionStatus> ACTIVE_STATUSES = EnumSet.of(
			MealSessionStatus.DRAFT,
			MealSessionStatus.SERVING,
			MealSessionStatus.LEFTOVER);

	private final MealSessionRepository mealSessionRepository;
	private final MealLineItemRepository mealLineItemRepository;
	private final TrackingEventRepository trackingEventRepository;
	private final MealPlanRepository mealPlanRepository;

	@Override
	public MealSessionResDto create(UUID actorUserId, CreateMealSessionReq request) {
		mealSessionRepository
				.findFirstByDonorInstitutionIdAndRegisteredByUserIdAndStatusInOrderByCreatedAtDesc(
						request.donorInstitutionId(), actorUserId, ACTIVE_STATUSES)
				.ifPresent(s -> {
					throw new IllegalStateException("An active meal session already exists. Close it before starting a new one.");
				});

		LocalDate sessionDate = request.sessionDate() != null ? request.sessionDate() : LocalDate.now();
		if (sessionDate.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Cannot start a live session for a future date");
		}
		List<LineItemInput> lineItems = resolveLineItems(request);
		validateLineItems(lineItems);
		BigDecimal preparedWeight = request.preparedWeightKg() != null
				? request.preparedWeightKg()
				: sumPrepared(lineItems);
		String menuName = buildMenuName(request.menuName(), lineItems, sessionDate, request.mealType());

		MealSession session = MealSession.builder()
				.donorInstitutionId(request.donorInstitutionId())
				.registeredByUserId(actorUserId)
				.mealType(request.mealType())
				.menuName(menuName)
				.sessionDate(sessionDate)
				.preparedWeightKg(preparedWeight)
				.headcount(request.headcount())
				.notes(request.notes())
				.build();
		mealSessionRepository.save(session);

		saveLineItems(session.getId(), lineItems);
		session.transitionTo(MealSessionStatus.SERVING);
		return toDto(session, true);
	}

	@Override
	@Transactional(readOnly = true)
	public MealSessionResDto getActive(UUID actorUserId, UUID donorInstitutionId) {
		MealSession session = mealSessionRepository
				.findFirstByDonorInstitutionIdAndRegisteredByUserIdAndStatusInOrderByCreatedAtDesc(
						donorInstitutionId, actorUserId, ACTIVE_STATUSES)
				.orElseThrow(() -> new EntityNotFoundException("No active meal session"));
		return toDto(session, true);
	}

	@Override
	@Transactional(readOnly = true)
	public MealSessionResDto getById(UUID actorUserId, UUID sessionId) {
		return toDto(requireOwnedSession(actorUserId, sessionId), true);
	}

	@Override
	public MealSessionResDto confirmEvent(UUID actorUserId, UUID sessionId, ConfirmTrackingEventReq request) {
		MealSession session = requireOwnedSession(actorUserId, sessionId);
		if (session.getStatus() == MealSessionStatus.CLOSED) {
			throw new IllegalStateException("Session is closed");
		}
		if (!TrackingEventValidation.hasMeaningfulPayload(request)) {
			throw new IllegalArgumentException(
					"Tracking event has no measurable weight (all zero). Not saved.");
		}

		BigDecimal effectiveDelta = TrackingEventValidation.resolveDeltaKg(request);

		if (request.mode() == TrackingMode.SERVING) {
			session.transitionTo(MealSessionStatus.SERVING);
			session.applyServingDelta(effectiveDelta);
		} else if (request.mode() == TrackingMode.LEFTOVER) {
			session.transitionTo(MealSessionStatus.LEFTOVER);
			applyLeftoverSnapshots(session.getId(), request.lineItems());
			if (request.lineItems() != null && !request.lineItems().isEmpty()) {
				BigDecimal totalLeftover = request.lineItems().stream()
						.map(LineItemSnapshot::kg)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				session.setRemainingWeight(totalLeftover);
			} else if (request.deltaKg() != null) {
				session.setRemainingWeight(request.deltaKg());
			}
		}

		String snapshotsJson = JsonUtil.toJson(request.lineItems());
		trackingEventRepository.save(TrackingEvent.builder()
				.sessionId(session.getId())
				.mode(request.mode())
				.source(request.source())
				.deltaKg(effectiveDelta)
				.lineItemSnapshots(snapshotsJson)
				.aiRawResponse(request.aiRawResponse())
				.build());

		return toDto(session, true);
	}

	@Override
	public MealSessionResDto close(UUID actorUserId, UUID sessionId) {
		MealSession session = requireOwnedSession(actorUserId, sessionId);
		session.close();
		return toDto(session, true);
	}

	private List<LineItemInput> resolveLineItems(CreateMealSessionReq request) {
		if (request.lineItems() != null && !request.lineItems().isEmpty()) {
			return request.lineItems();
		}
		if (request.mealPlanId() != null) {
			MealPlan plan = mealPlanRepository.findById(request.mealPlanId())
					.orElseThrow(() -> new EntityNotFoundException("Meal plan not found"));
			List<LineItemInput> fromPlan = JsonUtil.fromJson(plan.getLineItemsJson(), new TypeReference<>() {});
			if (fromPlan != null && !fromPlan.isEmpty()) {
				return fromPlan;
			}
		}
		throw new IllegalArgumentException("At least one dish is required");
	}

	private void validateLineItems(List<LineItemInput> items) {
		for (LineItemInput item : items) {
			if (item.itemName() == null || item.itemName().isBlank()) {
				throw new IllegalArgumentException("Dish name is required");
			}
			if (item.preparedKg() == null || item.preparedKg().signum() <= 0) {
				throw new IllegalArgumentException("Each dish must have prepared kg > 0");
			}
		}
	}

	private BigDecimal sumPrepared(List<LineItemInput> items) {
		return items.stream().map(LineItemInput::preparedKg).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private String buildMenuName(String menuName, List<LineItemInput> lineItems, LocalDate date, com.mealbridge.domain.donation.entity.MealType mealType) {
		if (menuName != null && !menuName.isBlank()) {
			return menuName.trim();
		}
		String dishes = lineItems.stream()
				.map(LineItemInput::itemName)
				.limit(3)
				.reduce((a, b) -> a + ", " + b)
				.orElse("Meal");
		if (lineItems.size() > 3) {
			dishes += "…";
		}
		return date + " " + mealType.name() + " — " + dishes;
	}

	private void saveLineItems(UUID sessionId, List<LineItemInput> inputs) {
		if (inputs == null) {
			return;
		}
		for (LineItemInput input : inputs) {
			mealLineItemRepository.save(MealLineItem.builder()
					.sessionId(sessionId)
					.itemName(input.itemName())
					.preparedKg(input.preparedKg())
					.perPersonKg(input.perPersonKg())
					.ingredients(input.ingredients())
					.build());
		}
	}

	private void applyLeftoverSnapshots(UUID sessionId, List<LineItemSnapshot> snapshots) {
		if (snapshots == null || snapshots.isEmpty()) {
			return;
		}
		List<MealLineItem> existing = mealLineItemRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
		for (LineItemSnapshot snapshot : snapshots) {
			MealLineItem match = existing.stream()
					.filter(item -> item.getItemName().equalsIgnoreCase(snapshot.itemName().trim()))
					.findFirst()
					.orElse(null);
			if (match != null) {
				match.setRemainingKg(snapshot.kg());
			} else {
				mealLineItemRepository.save(MealLineItem.builder()
						.sessionId(sessionId)
						.itemName(snapshot.itemName())
						.preparedKg(snapshot.kg())
						.perPersonKg(null)
						.ingredients(null)
						.build());
			}
		}
	}

	private MealSession requireOwnedSession(UUID actorUserId, UUID sessionId) {
		MealSession session = mealSessionRepository.findById(sessionId)
				.orElseThrow(() -> new EntityNotFoundException("Meal session not found"));
		if (!session.getRegisteredByUserId().equals(actorUserId)) {
			throw new SecurityException("Access denied");
		}
		return session;
	}

	private MealSessionResDto toDto(MealSession session, boolean includeChildren) {
		List<MealLineItemResDto> lineItems = List.of();
		List<TrackingEventResDto> events = List.of();
		if (includeChildren) {
			lineItems = mealLineItemRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
					.map(item -> new MealLineItemResDto(
							item.getId(),
							item.getItemName(),
							item.getPreparedKg(),
							item.getPerPersonKg(),
							item.getRemainingKg(),
							item.getIngredients()))
					.toList();
			events = trackingEventRepository.findTop20BySessionIdOrderByCreatedAtDesc(session.getId()).stream()
					.map(this::toEventDto)
					.toList();
		}
		return new MealSessionResDto(
				session.getId(),
				session.getDonorInstitutionId(),
				session.getStatus(),
				session.getMealType(),
				session.getMenuName(),
				session.getSessionDate(),
				session.getPreparedWeightKg(),
				session.getRemainingWeightKg(),
				session.getHeadcount(),
				session.getNotes(),
				lineItems,
				events,
				session.getCreatedAt(),
				session.getUpdatedAt());
	}

	private TrackingEventResDto toEventDto(TrackingEvent event) {
		List<LineItemSnapshot> snapshots = JsonUtil.fromJson(
				event.getLineItemSnapshots(),
				new TypeReference<List<LineItemSnapshot>>() {});
		if (snapshots == null) {
			snapshots = new ArrayList<>();
		}
		return new TrackingEventResDto(
				event.getId(),
				event.getMode(),
				event.getSource(),
				event.getDeltaKg(),
				snapshots,
				event.getReviewedAt(),
				event.getCreatedAt());
	}
}
