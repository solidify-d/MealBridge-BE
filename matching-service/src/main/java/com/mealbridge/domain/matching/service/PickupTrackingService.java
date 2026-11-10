package com.mealbridge.domain.matching.service;

import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.matching.client.DonationClient;
import com.mealbridge.domain.matching.client.DonationClient.DonationSnapshot;
import com.mealbridge.domain.matching.client.DonorLocationClient;
import com.mealbridge.domain.matching.client.DonorLocationClient.DonorLocation;
import com.mealbridge.domain.matching.client.DonorSettingsClient;
import com.mealbridge.domain.matching.client.OrganizationContactClient;
import com.mealbridge.domain.matching.client.OrganizationContactClient.OrganizationContact;
import com.mealbridge.domain.matching.dto.ActivePickupAssignmentResDto;
import com.mealbridge.domain.matching.dto.DonorPickupDetailResDto;
import com.mealbridge.domain.matching.dto.OptimizedRouteResDto;
import com.mealbridge.domain.matching.dto.OrganizationPickupHistoryLineItemResDto;
import com.mealbridge.domain.matching.dto.OrganizationPickupHistoryResDto;
import com.mealbridge.domain.matching.dto.RouteStopResDto;
import com.mealbridge.domain.matching.entity.OrganizationPickupWarning;
import com.mealbridge.domain.matching.entity.PickupAssignment;
import com.mealbridge.domain.matching.entity.PickupAssignmentStatus;
import com.mealbridge.domain.matching.entity.RoutePlan;
import com.mealbridge.domain.matching.entity.RoutePlanStatus;
import com.mealbridge.domain.matching.repository.OrganizationPickupWarningRepository;
import com.mealbridge.domain.matching.repository.PickupAssignmentRepository;
import com.mealbridge.domain.matching.repository.RoutePlanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PickupTrackingService {

	private final RoutePlanRepository routePlanRepository;
	private final PickupAssignmentRepository pickupAssignmentRepository;
	private final DonationClient donationClient;
	private final DonorLocationClient donorLocationClient;
	private final DonorSettingsClient donorSettingsClient;
	private final PickupNotificationService pickupNotificationService;
	private final OrganizationPickupWarningRepository organizationPickupWarningRepository;
	private final OrganizationContactClient organizationContactClient;

	@Transactional
	public OptimizedRouteResDto linkAssignmentsAfterConfirm(
			UUID routePlanId,
			UUID organizationId,
			List<RouteStopResDto> stops) {
		Map<UUID, LocalDateTime> arrivals = PickupEtaCalculator.estimateArrivalsNow(stops);
		for (RouteStopResDto stop : stops) {
			PickupAssignment assignment = pickupAssignmentRepository
					.findByDonationIdAndOrganizationId(stop.donationId(), organizationId)
					.orElseThrow(() -> new EntityNotFoundException(
							"Pickup assignment not found for donation: " + stop.donationId()));
			assignment.linkRoutePlan(routePlanId, arrivals.get(stop.donationId()));
			pickupAssignmentRepository.save(assignment);
		}
		pickupNotificationService.notifyPickupConfirmed(
				organizationId, routePlanId, stops.stream().map(RouteStopResDto::donationId).toList(), arrivals);
		RoutePlan routePlan = routePlanRepository.findById(routePlanId)
				.orElseThrow(() -> new EntityNotFoundException("Route plan not found"));
		return new OptimizedRouteResDto(
				routePlan.getId(),
				routePlan.getOrganizationId(),
				stops,
				routePlan.getTotalDistanceM(),
				routePlan.getTotalDurationS());
	}

	@Transactional
	public void startTransit(UUID routePlanId, UUID organizationId, UUID actorUserId) {
		RoutePlan routePlan = requireActiveRoute(routePlanId, organizationId);
		List<RouteStopResDto> stops = parseStops(routePlan.getPlannedStopsJson());
		Map<UUID, LocalDateTime> arrivals = PickupEtaCalculator.estimateArrivalsNow(stops);
		LocalDateTime now = KstDates.now();

		List<PickupAssignment> assignments = pickupAssignmentRepository
				.findByRoutePlanIdAndOrganizationId(routePlanId, organizationId);
		for (PickupAssignment assignment : assignments) {
			if (assignment.getStatus() != PickupAssignmentStatus.ACCEPTED) {
				continue;
			}
			donationClient.markInTransit(assignment.getDonationId(), organizationId, actorUserId);
			assignment.markInTransit(now);
			assignment.updateEstimatedArrival(arrivals.get(assignment.getDonationId()));
		}

		pickupNotificationService.notifyPickupEnRoute(
				organizationId,
				routePlanId,
				assignments.stream().map(PickupAssignment::getDonationId).toList(),
				arrivals);
	}

	@Transactional
	public void completeRoute(UUID routePlanId, UUID organizationId, UUID actorUserId) {
		RoutePlan routePlan = requireActiveRoute(routePlanId, organizationId);
		List<PickupAssignment> assignments = pickupAssignmentRepository
				.findByRoutePlanIdAndOrganizationId(routePlanId, organizationId);
		LocalDateTime now = KstDates.now();

		for (PickupAssignment assignment : assignments) {
			if (assignment.getStatus() != PickupAssignmentStatus.IN_TRANSIT) {
				continue;
			}
			donationClient.completeDonation(assignment.getDonationId(), organizationId, actorUserId);
			assignment.complete(now);
			pickupNotificationService.notifyPickupCompleted(organizationId, assignment.getDonationId());
		}
		routePlan.complete();
	}

	@Transactional
	public void cancelInTransit(UUID assignmentId, UUID organizationId, UUID actorUserId, String reason) {
		PickupAssignment assignment = pickupAssignmentRepository.findById(assignmentId)
				.orElseThrow(() -> new EntityNotFoundException("Pickup assignment not found"));
		if (!assignment.getOrganizationId().equals(organizationId)) {
			throw new SecurityException("Access denied");
		}
		if (assignment.getStatus() != PickupAssignmentStatus.IN_TRANSIT) {
			throw new IllegalStateException("Only in-transit pickups can be cancelled");
		}

		donationClient.releaseDonation(assignment.getDonationId(), organizationId, actorUserId);
		assignment.cancel(KstDates.now());
		String resolvedReason = reason != null && !reason.isBlank() ? reason : "Cancelled while en route";
		pickupNotificationService.notifyPickupCancelled(organizationId, assignment.getDonationId(), resolvedReason);
		pickupNotificationService.recordWarningAndNotifyAdminsIfNeeded(
				organizationId, assignment.getId(), assignment.getDonationId(), resolvedReason);
	}

	@Transactional(readOnly = true)
	public List<ActivePickupAssignmentResDto> listActive(UUID organizationId) {
		List<PickupAssignmentStatus> statuses = List.of(
				PickupAssignmentStatus.ACCEPTED,
				PickupAssignmentStatus.IN_TRANSIT);
		return pickupAssignmentRepository
				.findByOrganizationIdAndRoutePlanIdIsNotNullAndStatusInOrderByCreatedAtDesc(organizationId, statuses)
				.stream()
				.map(this::toActiveDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public DonorPickupDetailResDto getDonorPickupDetail(UUID donationId, UUID actorUserId) {
		DonationSnapshot donation = donationClient.fetchById(donationId);
		if (!donation.registeredByUserId().equals(actorUserId)) {
			throw new SecurityException("Access denied");
		}

		PickupAssignment assignment = resolvePickupAssignment(donation).orElse(null);
		if (assignment == null) {
			return buildDonorPickupDetailFromDonation(donation);
		}

		OrganizationContact contact = organizationContactClient.fetchContact(assignment.getOrganizationId());
		String cancelReason = organizationPickupWarningRepository
				.findByPickupAssignmentId(assignment.getId())
				.map(OrganizationPickupWarning::getReason)
				.orElse(null);

		return new DonorPickupDetailResDto(
				assignment.getId(),
				assignment.getOrganizationId(),
				contact.organizationName(),
				contact.organizationPhone(),
				contact.contactPersonName(),
				assignment.getStatus(),
				donation.acceptedAt() != null ? donation.acceptedAt() : assignment.getCreatedAt(),
				assignment.getEstimatedArrivalAt(),
				coalesceTimestamp(assignment.getInTransitAt(), donation.inTransitAt()),
				coalesceTimestamp(assignment.getCompletedAt(), donation.completedAt()),
				assignment.getCancelledAt(),
				cancelReason);
	}

	@Transactional(readOnly = true)
	public List<OrganizationPickupHistoryResDto> listHistory(UUID organizationId) {
		List<PickupAssignmentStatus> statuses = List.of(
				PickupAssignmentStatus.ACCEPTED,
				PickupAssignmentStatus.IN_TRANSIT,
				PickupAssignmentStatus.COMPLETED,
				PickupAssignmentStatus.CANCELLED);
		List<PickupAssignment> assignments = pickupAssignmentRepository
				.findByOrganizationIdAndStatusInOrderByUpdatedAtDesc(organizationId, statuses);

		Map<UUID, DonorLocation> donorLocations = new HashMap<>();
		Map<UUID, Boolean> sessionBulkByDonor = new HashMap<>();

		return assignments.stream()
				.map(assignment -> toHistoryDto(assignment, donorLocations, sessionBulkByDonor))
				.toList();
	}

	public void validateSessionBulkSelection(UUID organizationId, List<UUID> donationIds) {
		List<DonationSnapshot> allRegistered = donationClient.fetchRegisteredAvailable();
		Set<UUID> selected = new HashSet<>(donationIds);

		for (UUID donationId : donationIds) {
			DonationSnapshot donation = allRegistered.stream()
					.filter(item -> item.id().equals(donationId))
					.findFirst()
					.orElseGet(() -> donationClient.fetchById(donationId));

			if (donation.mealSessionId() == null) {
				continue;
			}
			if (!donorSettingsClient.fetchSettings(donation.donorInstitutionId()).isSessionBulk()) {
				continue;
			}

			List<UUID> sessionDonationIds = allRegistered.stream()
					.filter(item -> donation.mealSessionId().equals(item.mealSessionId()))
					.map(DonationSnapshot::id)
					.toList();

			if (!selected.containsAll(sessionDonationIds)) {
				throw new IllegalArgumentException(
						"Session bulk pickup requires all surplus from the same serving session to be included");
			}
		}
	}

	public List<UUID> expandSessionBulkDonations(List<UUID> seedDonationIds) {
		List<DonationSnapshot> allRegistered = donationClient.fetchRegisteredAvailable();
		Set<UUID> expanded = new HashSet<>(seedDonationIds);

		for (UUID donationId : new ArrayList<>(seedDonationIds)) {
			DonationSnapshot donation = allRegistered.stream()
					.filter(item -> item.id().equals(donationId))
					.findFirst()
					.orElse(null);
			if (donation == null || donation.mealSessionId() == null) {
				continue;
			}
			if (!donorSettingsClient.fetchSettings(donation.donorInstitutionId()).isSessionBulk()) {
				continue;
			}
			allRegistered.stream()
					.filter(item -> donation.mealSessionId().equals(item.mealSessionId()))
					.map(DonationSnapshot::id)
					.forEach(expanded::add);
		}
		return expanded.stream().toList();
	}

	private RoutePlan requireActiveRoute(UUID routePlanId, UUID organizationId) {
		RoutePlan routePlan = routePlanRepository.findById(routePlanId)
				.orElseThrow(() -> new EntityNotFoundException("Route plan not found"));
		if (!routePlan.getOrganizationId().equals(organizationId)) {
			throw new SecurityException("Access denied");
		}
		if (routePlan.getStatus() != RoutePlanStatus.ACTIVE) {
			throw new IllegalStateException("Route plan is not active");
		}
		return routePlan;
	}

	private List<RouteStopResDto> parseStops(String json) {
		return JsonUtil.fromJson(json, new TypeReference<List<RouteStopResDto>>() {
		});
	}

	private java.util.Optional<PickupAssignment> resolvePickupAssignment(DonationSnapshot donation) {
		if (donation.acceptedOrganizationId() != null) {
			java.util.Optional<PickupAssignment> assigned = pickupAssignmentRepository
					.findByDonationIdAndOrganizationId(donation.id(), donation.acceptedOrganizationId());
			if (assigned.isPresent()) {
				return assigned;
			}
		}

		return pickupAssignmentRepository.findByDonationIdOrderByScoreDesc(donation.id()).stream()
				.filter(assignment -> assignment.getStatus() != PickupAssignmentStatus.RECOMMENDED)
				.max(java.util.Comparator.comparing(PickupAssignment::getUpdatedAt));
	}

	private DonorPickupDetailResDto buildDonorPickupDetailFromDonation(DonationSnapshot donation) {
		if (donation.acceptedOrganizationId() == null) {
			return null;
		}
		OrganizationContact contact = organizationContactClient.fetchContact(donation.acceptedOrganizationId());
		return new DonorPickupDetailResDto(
				null,
				donation.acceptedOrganizationId(),
				contact.organizationName(),
				contact.organizationPhone(),
				contact.contactPersonName(),
				mapAssignmentStatusFromDonation(donation.status()),
				donation.acceptedAt(),
				null,
				donation.inTransitAt(),
				donation.completedAt(),
				null,
				null);
	}

	private PickupAssignmentStatus mapAssignmentStatusFromDonation(String status) {
		if (status == null) {
			return PickupAssignmentStatus.ACCEPTED;
		}
		return switch (status) {
			case "IN_TRANSIT" -> PickupAssignmentStatus.IN_TRANSIT;
			case "COMPLETED" -> PickupAssignmentStatus.COMPLETED;
			case "CANCELLED" -> PickupAssignmentStatus.CANCELLED;
			default -> PickupAssignmentStatus.ACCEPTED;
		};
	}

	private LocalDateTime coalesceTimestamp(LocalDateTime primary, LocalDateTime fallback) {
		return primary != null ? primary : fallback;
	}

	private ActivePickupAssignmentResDto toActiveDto(PickupAssignment assignment) {
		DonationSnapshot donation = donationClient.fetchById(assignment.getDonationId());
		return new ActivePickupAssignmentResDto(
				assignment.getId(),
				assignment.getDonationId(),
				assignment.getOrganizationId(),
				assignment.getRoutePlanId(),
				assignment.getStatus(),
				donation.menuName(),
				assignment.getEstimatedArrivalAt(),
				assignment.getInTransitAt(),
				assignment.getCompletedAt());
	}

	private OrganizationPickupHistoryResDto toHistoryDto(
			PickupAssignment assignment,
			Map<UUID, DonorLocation> donorLocations,
			Map<UUID, Boolean> sessionBulkByDonor) {
		DonationSnapshot donation = donationClient.fetchById(assignment.getDonationId());
		DonorLocation donorLocation = donorLocations.computeIfAbsent(
				donation.donorInstitutionId(), id -> donorLocationClient.fetchLocation(id));
		boolean sessionBulk = sessionBulkByDonor.computeIfAbsent(
				donation.donorInstitutionId(),
				id -> donorSettingsClient.fetchSettings(id).isSessionBulk());
		String cancelReason = organizationPickupWarningRepository
				.findByPickupAssignmentId(assignment.getId())
				.map(OrganizationPickupWarning::getReason)
				.orElse(null);

		return new OrganizationPickupHistoryResDto(
				assignment.getId(),
				assignment.getDonationId(),
				assignment.getOrganizationId(),
				assignment.getRoutePlanId(),
				assignment.getStatus(),
				donation.status(),
				donation.menuName(),
				donation.mealType(),
				donation.totalWeightKg(),
				donation.notes(),
				donation.beforeImageUrl(),
				donation.afterImageUrl(),
				parseLineItems(donation.lineItemsJson()),
				donation.donorInstitutionId(),
				donorLocation.name(),
				donorLocation.address(),
				donation.mealSessionId(),
				sessionBulk && donation.mealSessionId() != null,
				assignment.getScore(),
				assignment.getEstimatedArrivalAt(),
				assignment.getInTransitAt(),
				assignment.getCompletedAt(),
				assignment.getCancelledAt(),
				cancelReason,
				donation.acceptedAt(),
				assignment.getCreatedAt(),
				assignment.getUpdatedAt());
	}

	private List<OrganizationPickupHistoryLineItemResDto> parseLineItems(String lineItemsJson) {
		if (lineItemsJson == null || lineItemsJson.isBlank()) {
			return Collections.emptyList();
		}
		List<LineItemJson> items = JsonUtil.fromJson(lineItemsJson, new TypeReference<List<LineItemJson>>() {
		});
		if (items == null) {
			return Collections.emptyList();
		}
		return items.stream()
				.map(item -> new OrganizationPickupHistoryLineItemResDto(
						item.itemName,
						item.preparedKg,
						item.perPersonKg,
						item.ingredients,
						item.nutritionJson,
						item.allergensJson))
				.toList();
	}

	private record LineItemJson(
			String itemName,
			BigDecimal preparedKg,
			BigDecimal perPersonKg,
			String ingredients,
			String nutritionJson,
			String allergensJson) {
	}
}
