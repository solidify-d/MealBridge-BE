package com.mealbridge.domain.matching.service;

import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.matching.client.OrganizationClient;
import com.mealbridge.domain.matching.client.OrganizationClient.OrganizationSnapshot;
import com.mealbridge.domain.matching.dto.ConfirmPickupPlanReq;
import com.mealbridge.domain.matching.dto.OptimizedRouteResDto;
import com.mealbridge.domain.matching.dto.PickupPlanItemResDto;
import com.mealbridge.domain.matching.dto.PickupPlanResDto;
import com.mealbridge.domain.matching.dto.RecommendationResDto;
import com.mealbridge.domain.matching.dto.SuggestPickupPlanReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PickupPlanService {

	private static final double DEFAULT_MAX_DISTANCE_KM = 30.0;
	private static final int MAX_STOPS = 8;
	private static final long STOP_BUFFER_SECONDS = 600;

	private final RecommendationService recommendationService;
	private final RouteOptimizationService routeOptimizationService;
	private final PickupAssignmentService pickupAssignmentService;
	private final OrganizationClient organizationClient;
	private final PickupTrackingService pickupTrackingService;

	@Transactional(readOnly = true)
	public PickupPlanResDto suggest(SuggestPickupPlanReq request) {
		OrganizationSnapshot organization = organizationClient.fetchById(request.organizationId());
		double maxDistanceKm = coalesce(request.maxPickupDistanceKm(), organization.maxPickupDistanceKm(), DEFAULT_MAX_DISTANCE_KM);
		Double desiredWeightKg = coalesceNullable(request.desiredPickupWeightKg(), organization.desiredPickupWeightKg());
		String servingStartTimeStr = coalesceString(request.servingStartTime(), organization.servingStartTime());

		List<RecommendationResDto> ranked = recommendationService.recommendAllDonationsForOrganization(request.organizationId())
				.stream()
				.filter(rec -> rec.distanceKm() == null || rec.distanceKm() <= maxDistanceKm)
				.filter(rec -> rec.hoursUntilDeadline() == null || rec.hoursUntilDeadline() > 0)
				.sorted(Comparator.comparingDouble(RecommendationResDto::score).reversed())
				.toList();

		if (ranked.isEmpty()) {
			return emptyPlan(request.organizationId(), servingStartTimeStr,
					"No surplus meals match your distance and deadline preferences right now.");
		}

		List<RecommendationResDto> selected;
		boolean manualSelection = request.donationIds() != null && !request.donationIds().isEmpty();
		if (manualSelection) {
			selected = selectManualMeals(ranked, request.donationIds());
			if (selected.isEmpty()) {
				return emptyPlan(request.organizationId(), servingStartTimeStr,
						"Selected meals are not available or no longer match your preferences.");
			}
			selected = applySessionBulkExpansion(ranked, selected);
			if (selected.size() > MAX_STOPS) {
				return emptyPlan(request.organizationId(), servingStartTimeStr,
						"Too many meals selected. Maximum " + MAX_STOPS + " stops per route.");
			}
		} else {
			selected = selectMeals(ranked, desiredWeightKg);
			if (selected.isEmpty()) {
				return emptyPlan(request.organizationId(), servingStartTimeStr,
						"No suitable meals found for your desired pickup weight.");
			}
			selected = trimForServingWindow(request.organizationId(), selected, servingStartTimeStr);
			if (selected.isEmpty()) {
				return emptyPlan(request.organizationId(), servingStartTimeStr,
						"Could not fit a pickup route before your serving start time. Try a later serving time or fewer meals.");
			}
			selected = applySessionBulkExpansion(ranked, selected);
			selected = trimForServingWindow(request.organizationId(), selected, servingStartTimeStr);
			if (selected.isEmpty()) {
				return emptyPlan(request.organizationId(), servingStartTimeStr,
						"Could not fit a pickup route before your serving start time. Try a later serving time or fewer meals.");
			}
		}

		List<UUID> donationIds = selected.stream().map(RecommendationResDto::donationId).toList();
		OptimizedRouteResDto route = routeOptimizationService.optimize(request.organizationId(), donationIds, false);
		long roundTripDurationS = route.totalDurationS() * 2 + route.stops().size() * STOP_BUFFER_SECONDS;

		List<PickupPlanItemResDto> items = selected.stream()
				.map(rec -> new PickupPlanItemResDto(
						rec.donationId(),
						rec.donationMenuName(),
						rec.totalWeightKg(),
						rec.score(),
						rec.distanceKm(),
						rec.hoursUntilDeadline()))
				.toList();

		String servingWarning = servingWindowWarning(roundTripDurationS, servingStartTimeStr);
		String summary = manualSelection
				? buildManualSummary(selected.size(), route, roundTripDurationS, servingStartTimeStr, maxDistanceKm)
						+ servingWarning
				: buildSummary(selected.size(), route, roundTripDurationS, servingStartTimeStr, maxDistanceKm);

		return new PickupPlanResDto(
				request.organizationId(),
				items,
				route,
				roundTripDurationS,
				servingStartTimeStr,
				summary,
				true);
	}

	@Transactional
	public OptimizedRouteResDto confirm(UUID actorUserId, ConfirmPickupPlanReq request) {
		List<UUID> donationIds = pickupTrackingService.expandSessionBulkDonations(request.donationIds());
		pickupTrackingService.validateSessionBulkSelection(request.organizationId(), donationIds);
		ConfirmPickupPlanReq expandedRequest = new ConfirmPickupPlanReq(request.organizationId(), donationIds);
		pickupAssignmentService.confirmPlan(actorUserId, expandedRequest);
		OptimizedRouteResDto route = routeOptimizationService.optimize(
				request.organizationId(), donationIds, true);
		if (route.routePlanId() == null) {
			return route;
		}
		return pickupTrackingService.linkAssignmentsAfterConfirm(
				route.routePlanId(), request.organizationId(), route.stops());
	}

	private List<RecommendationResDto> applySessionBulkExpansion(
			List<RecommendationResDto> ranked, List<RecommendationResDto> selected) {
		List<UUID> expandedIds = pickupTrackingService.expandSessionBulkDonations(
				selected.stream().map(RecommendationResDto::donationId).toList());
		return ranked.stream()
				.filter(rec -> expandedIds.contains(rec.donationId()))
				.toList();
	}

	private List<RecommendationResDto> selectManualMeals(
			List<RecommendationResDto> ranked, List<UUID> donationIds) {
		List<RecommendationResDto> selected = new ArrayList<>();
		for (UUID donationId : donationIds) {
			ranked.stream()
					.filter(rec -> rec.donationId().equals(donationId))
					.findFirst()
					.ifPresent(selected::add);
		}
		return selected;
	}

	private List<RecommendationResDto> selectMeals(List<RecommendationResDto> ranked, Double desiredWeightKg) {
		List<RecommendationResDto> selected = new ArrayList<>();
		double accumulatedKg = 0;
		for (RecommendationResDto rec : ranked) {
			if (selected.size() >= MAX_STOPS) {
				break;
			}
			selected.add(rec);
			if (desiredWeightKg != null && desiredWeightKg > 0) {
				double mealKg = rec.totalWeightKg() != null ? rec.totalWeightKg().doubleValue() : 10.0;
				accumulatedKg += mealKg;
				if (accumulatedKg >= desiredWeightKg) {
					break;
				}
			}
		}
		return selected;
	}

	private List<RecommendationResDto> trimForServingWindow(
			UUID organizationId, List<RecommendationResDto> selected, String servingStartTimeStr) {
		if (servingStartTimeStr == null || servingStartTimeStr.isBlank()) {
			return selected;
		}
		LocalTime servingStart = LocalTime.parse(servingStartTimeStr);
		LocalDateTime deadline = LocalDateTime.of(KstDates.now().toLocalDate(), servingStart);
		if (deadline.isBefore(KstDates.now())) {
			deadline = deadline.plusDays(1);
		}

		List<RecommendationResDto> current = new ArrayList<>(selected);
		while (!current.isEmpty()) {
			List<UUID> ids = current.stream().map(RecommendationResDto::donationId).toList();
			OptimizedRouteResDto route = routeOptimizationService.optimize(organizationId, ids, false);
			long roundTrip = route.totalDurationS() * 2 + route.stops().size() * STOP_BUFFER_SECONDS;
			LocalDateTime returnTime = KstDates.now().plusSeconds(roundTrip);
			if (!returnTime.isAfter(deadline)) {
				return current;
			}
			current.remove(current.size() - 1);
		}
		return List.of();
	}

	private String servingWindowWarning(long roundTripDurationS, String servingStartTimeStr) {
		if (servingStartTimeStr == null || servingStartTimeStr.isBlank()) {
			return "";
		}
		LocalTime servingStart = LocalTime.parse(servingStartTimeStr);
		LocalDateTime deadline = LocalDateTime.of(KstDates.now().toLocalDate(), servingStart);
		if (deadline.isBefore(KstDates.now())) {
			deadline = deadline.plusDays(1);
		}
		LocalDateTime returnTime = KstDates.now().plusSeconds(roundTripDurationS);
		if (returnTime.isAfter(deadline)) {
			return " Note: this route may return after your serving start time ("
					+ formatDuration(roundTripDurationS)
					+ " round trip).";
		}
		return "";
	}

	private PickupPlanResDto emptyPlan(UUID organizationId, String servingStartTime, String summary) {
		return new PickupPlanResDto(
				organizationId,
				List.of(),
				new OptimizedRouteResDto(null, organizationId, List.of(), 0, 0),
				0,
				servingStartTime,
				summary,
				false);
	}

	private String buildSummary(
			int mealCount,
			OptimizedRouteResDto route,
			long roundTripDurationS,
			String servingStartTime,
			double maxDistanceKm) {
		long minutes = Math.round(roundTripDurationS / 60.0);
		StringBuilder sb = new StringBuilder();
		sb.append("We selected ").append(mealCount).append(" meal");
		if (mealCount != 1) {
			sb.append('s');
		}
		sb.append(" within ").append(String.format("%.0f", maxDistanceKm)).append(" km");
		sb.append(", optimized for a ").append(formatDuration(roundTripDurationS)).append(" round trip");
		if (servingStartTime != null && !servingStartTime.isBlank()) {
			sb.append(" so you can start serving at ").append(servingStartTime);
		}
		sb.append(". Review the route below, then confirm when ready.");
		return sb.toString();
	}

	private String buildManualSummary(
			int mealCount,
			OptimizedRouteResDto route,
			long roundTripDurationS,
			String servingStartTime,
			double maxDistanceKm) {
		StringBuilder sb = new StringBuilder();
		sb.append("You selected ").append(mealCount).append(" meal");
		if (mealCount != 1) {
			sb.append('s');
		}
		sb.append(" within ").append(String.format("%.0f", maxDistanceKm)).append(" km");
		sb.append(", optimized for a ").append(formatDuration(roundTripDurationS)).append(" round trip");
		if (servingStartTime != null && !servingStartTime.isBlank()) {
			sb.append(" so you can start serving at ").append(servingStartTime);
		}
		sb.append(". Review the route below, then confirm when ready.");
		return sb.toString();
	}

	private String formatDuration(long seconds) {
		long minutes = Math.round(seconds / 60.0);
		if (minutes < 60) {
			return minutes + " min";
		}
		long hours = minutes / 60;
		long rem = minutes % 60;
		return rem > 0 ? hours + " h " + rem + " min" : hours + " h";
	}

	private double coalesce(Double override, Double stored, double defaultValue) {
		if (override != null && override > 0) {
			return override;
		}
		if (stored != null && stored > 0) {
			return stored;
		}
		return defaultValue;
	}

	private Double coalesceNullable(Double override, Double stored) {
		if (override != null && override > 0) {
			return override;
		}
		return stored;
	}

	private String coalesceString(String override, String stored) {
		if (override != null && !override.isBlank()) {
			return override;
		}
		return stored;
	}
}
