package com.mealbridge.domain.mealplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealplan.dto.BulkMealPlanReq;
import com.mealbridge.domain.mealplan.dto.MealPlanResDto;
import com.mealbridge.domain.mealplan.dto.UpsertMealPlanReq;
import com.mealbridge.domain.mealplan.entity.MealPlan;
import com.mealbridge.domain.mealplan.repository.MealPlanRepository;
import com.mealbridge.domain.mealsession.dto.LineItemInput;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MealPlanServiceImpl implements MealPlanService {

	private final MealPlanRepository mealPlanRepository;

	@Override
	@Transactional(readOnly = true)
	public List<MealPlanResDto> list(UUID actorUserId, UUID donorInstitutionId, LocalDate from, LocalDate to) {
		return mealPlanRepository
				.findByDonorInstitutionIdAndPlanDateBetweenOrderByPlanDateAscMealTypeAsc(
						donorInstitutionId, from, to)
				.stream()
				.map(this::toDto)
				.toList();
	}

	@Override
	public MealPlanResDto upsert(UUID actorUserId, UpsertMealPlanReq request) {
		validateLineItems(request.lineItems());
		BigDecimal total = sumPrepared(request.lineItems());
		String json = JsonUtil.toJson(request.lineItems());

		MealPlan plan = mealPlanRepository
				.findByDonorInstitutionIdAndPlanDateAndMealType(
						request.donorInstitutionId(), request.planDate(), request.mealType())
				.orElse(null);

		if (plan == null) {
			plan = mealPlanRepository.save(MealPlan.builder()
					.donorInstitutionId(request.donorInstitutionId())
					.registeredByUserId(actorUserId)
					.mealType(request.mealType())
					.planDate(request.planDate())
					.preparedWeightKg(total)
					.headcount(request.headcount())
					.lineItemsJson(json)
					.notes(request.notes())
					.build());
		} else {
			plan.updateContents(total, request.headcount(), json, request.notes());
		}
		return toDto(plan);
	}

	@Override
	public List<MealPlanResDto> bulkUpsert(UUID actorUserId, BulkMealPlanReq request) {
		List<MealPlanResDto> results = new ArrayList<>();
		for (UpsertMealPlanReq plan : request.plans()) {
			results.add(upsert(
					actorUserId,
					new UpsertMealPlanReq(
							request.donorInstitutionId(),
							plan.planDate(),
							plan.mealType(),
							plan.headcount(),
							plan.lineItems(),
							plan.notes())));
		}
		return results;
	}

	@Override
	@Transactional(readOnly = true)
	public MealPlanResDto getForDate(
			UUID actorUserId, UUID donorInstitutionId, LocalDate planDate, MealType mealType) {
		MealPlan plan = mealPlanRepository
				.findByDonorInstitutionIdAndPlanDateAndMealType(donorInstitutionId, planDate, mealType)
				.orElseThrow(() -> new EntityNotFoundException("Meal plan not found"));
		return toDto(plan);
	}

	@Override
	public void delete(UUID actorUserId, UUID donorInstitutionId, UUID planId) {
		MealPlan plan = mealPlanRepository.findById(planId)
				.orElseThrow(() -> new EntityNotFoundException("Meal plan not found"));
		if (!plan.getDonorInstitutionId().equals(donorInstitutionId)) {
			throw new SecurityException("Access denied");
		}
		mealPlanRepository.delete(plan);
	}

	private void validateLineItems(List<LineItemInput> items) {
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("At least one dish is required");
		}
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

	private MealPlanResDto toDto(MealPlan plan) {
		List<LineItemInput> items = JsonUtil.fromJson(plan.getLineItemsJson(), new TypeReference<>() {});
		if (items == null) {
			items = List.of();
		}
		return new MealPlanResDto(
				plan.getId(),
				plan.getDonorInstitutionId(),
				plan.getPlanDate(),
				plan.getMealType(),
				plan.getPreparedWeightKg(),
				plan.getHeadcount(),
				items,
				plan.getNotes(),
				plan.getCreatedAt(),
				plan.getUpdatedAt());
	}
}
