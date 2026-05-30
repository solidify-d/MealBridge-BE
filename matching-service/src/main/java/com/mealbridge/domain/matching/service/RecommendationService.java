package com.mealbridge.domain.matching.service;

import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.matching.client.DonationClient;
import com.mealbridge.domain.matching.client.DonationClient.DonationSnapshot;
import com.mealbridge.domain.matching.client.DonorLocationClient;
import com.mealbridge.domain.matching.client.DonorLocationClient.DonorLocation;
import com.mealbridge.domain.matching.client.OrganizationClient;
import com.mealbridge.domain.matching.client.OrganizationClient.OrganizationSnapshot;
import com.mealbridge.domain.matching.dto.RecommendationResDto;
import com.mealbridge.domain.matching.entity.PickupAssignment;
import com.mealbridge.domain.matching.entity.PickupAssignmentStatus;
import com.mealbridge.domain.matching.repository.PickupAssignmentRepository;
import com.mealbridge.common.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

	private static final int TOP_N = 10;
	private static final double WEIGHT_DISTANCE = 0.35;
	private static final double WEIGHT_CAPACITY = 0.25;
	private static final double WEIGHT_DEADLINE = 0.25;
	private static final double WEIGHT_SERVICE_AREA = 0.15;
	private static final double DEFAULT_PORTION_KG = 0.3;

	private final DonationClient donationClient;
	private final OrganizationClient organizationClient;
	private final DonorLocationClient donorLocationClient;
	private final PickupAssignmentRepository pickupAssignmentRepository;

	public List<RecommendationResDto> recommendOrganizationsForDonation(UUID donationId) {
		DonationSnapshot donation = donationClient.fetchById(donationId);
		DonorLocation donorLocation = donorLocationClient.fetchLocation(donation.donorInstitutionId());
		List<OrganizationSnapshot> organizations = organizationClient.fetchActiveOrganizations();

		List<ScoredRecommendation> scored = new ArrayList<>();
		for (OrganizationSnapshot org : organizations) {
			double score = scoreDonationForOrganization(donation, donorLocation, org);
			Double distanceKm = distanceKm(donorLocation, org);
			Integer hoursUntilDeadline = hoursUntilDeadline(donation.pickupDeadlineAt());
			scored.add(new ScoredRecommendation(org, donation, score, distanceKm, hoursUntilDeadline));
		}

		return persistAndMapDonationRecommendations(donationId, scored);
	}

	public List<RecommendationResDto> recommendDonationsForOrganization(UUID organizationId) {
		return recommendAllDonationsForOrganization(organizationId).stream()
				.limit(TOP_N)
				.toList();
	}

	public List<RecommendationResDto> recommendAllDonationsForOrganization(UUID organizationId) {
		OrganizationSnapshot organization = organizationClient.fetchById(organizationId);
		List<DonationSnapshot> donations = donationClient.fetchRegisteredAvailable();

		List<ScoredRecommendation> scored = new ArrayList<>();
		for (DonationSnapshot donation : donations) {
			DonorLocation donorLocation = donorLocationClient.fetchLocation(donation.donorInstitutionId());
			double score = scoreDonationForOrganization(donation, donorLocation, organization);
			Double distanceKm = distanceKm(donorLocation, organization);
			Integer hoursUntilDeadline = hoursUntilDeadline(donation.pickupDeadlineAt());
			scored.add(new ScoredRecommendation(organization, donation, score, distanceKm, hoursUntilDeadline));
		}

		return scored.stream()
				.sorted(Comparator.comparingDouble(ScoredRecommendation::score).reversed())
				.map(this::toDto)
				.toList();
	}

	@Transactional
	protected List<RecommendationResDto> persistAndMapDonationRecommendations(
			UUID donationId, List<ScoredRecommendation> scored) {
		return scored.stream()
				.sorted(Comparator.comparingDouble(ScoredRecommendation::score).reversed())
				.limit(TOP_N)
				.map(item -> {
					PickupAssignment assignment = pickupAssignmentRepository
							.findByDonationIdAndOrganizationId(donationId, item.organization().id())
							.orElseGet(() -> PickupAssignment.builder()
									.donationId(donationId)
									.organizationId(item.organization().id())
									.status(PickupAssignmentStatus.RECOMMENDED)
									.score(item.score())
									.build());
					if (assignment.getStatus() == PickupAssignmentStatus.RECOMMENDED) {
						pickupAssignmentRepository.save(assignment);
					}
					return toDto(item);
				})
				.toList();
	}

	private double scoreDonationForOrganization(
			DonationSnapshot donation, DonorLocation donorLocation, OrganizationSnapshot organization) {
		double distanceScore = distanceScore(donorLocation, organization);
		double capacityScore = capacityScore(donation.totalWeightKg(), organization.recipientCapacity());
		double deadlineScore = deadlineScore(donation.pickupDeadlineAt());
		double serviceAreaScore = serviceAreaScore(donorLocation.address(), donation.notes(), organization.serviceArea());

		return (distanceScore * WEIGHT_DISTANCE
				+ capacityScore * WEIGHT_CAPACITY
				+ deadlineScore * WEIGHT_DEADLINE
				+ serviceAreaScore * WEIGHT_SERVICE_AREA) * 100.0;
	}

	private double distanceScore(DonorLocation donor, OrganizationSnapshot org) {
		if (donor.latitude() == null || donor.longitude() == null
				|| org.latitude() == null || org.longitude() == null) {
			return 0.5;
		}
		double meters = HaversineUtil.distanceMeters(
				donor.latitude(), donor.longitude(), org.latitude(), org.longitude());
		double km = meters / 1000.0;
		return 1.0 / (1.0 + km);
	}

	private double capacityScore(BigDecimal totalWeightKg, Integer recipientCapacity) {
		if (recipientCapacity == null || recipientCapacity <= 0) {
			return 0.5;
		}
		double estimatedPortions = totalWeightKg != null
				? totalWeightKg.doubleValue() / DEFAULT_PORTION_KG
				: 10.0;
		double ratio = estimatedPortions / recipientCapacity;
		if (ratio <= 0) {
			return 0.2;
		}
		if (ratio <= 1.0) {
			return 1.0;
		}
		return Math.max(0.1, 1.0 / ratio);
	}

	private double deadlineScore(LocalDateTime pickupDeadlineAt) {
		if (pickupDeadlineAt == null) {
			return 0.5;
		}
		long hours = Duration.between(KstDates.now(), pickupDeadlineAt).toHours();
		if (hours <= 0) {
			return 0.0;
		}
		if (hours >= 24) {
			return 0.3;
		}
		return 1.0 - (hours / 24.0) * 0.7;
	}

	private double serviceAreaScore(String donorAddress, String notes, String serviceArea) {
		if (serviceArea == null || serviceArea.isBlank()) {
			return 0.5;
		}
		String area = serviceArea.toLowerCase(Locale.ROOT);
		String address = donorAddress != null ? donorAddress.toLowerCase(Locale.ROOT) : "";
		String donationNotes = notes != null ? notes.toLowerCase(Locale.ROOT) : "";
		if (address.contains(area) || donationNotes.contains(area)) {
			return 1.0;
		}
		for (String token : area.split("[\\s,]+")) {
			if (token.length() >= 2 && (address.contains(token) || donationNotes.contains(token))) {
				return 0.8;
			}
		}
		return 0.2;
	}

	private Double distanceKm(DonorLocation donor, OrganizationSnapshot org) {
		if (donor.latitude() == null || donor.longitude() == null
				|| org.latitude() == null || org.longitude() == null) {
			return null;
		}
		return HaversineUtil.distanceMeters(
				donor.latitude(), donor.longitude(), org.latitude(), org.longitude()) / 1000.0;
	}

	private Integer hoursUntilDeadline(LocalDateTime pickupDeadlineAt) {
		if (pickupDeadlineAt == null) {
			return null;
		}
		return (int) Duration.between(KstDates.now(), pickupDeadlineAt).toHours();
	}

	private RecommendationResDto toDto(ScoredRecommendation item) {
		return new RecommendationResDto(
				item.organization().id(),
				item.organization().name(),
				item.donation().id(),
				item.donation().menuName(),
				item.donation().totalWeightKg(),
				item.score(),
				item.distanceKm(),
				item.hoursUntilDeadline());
	}

	private record ScoredRecommendation(
			OrganizationSnapshot organization,
			DonationSnapshot donation,
			double score,
			Double distanceKm,
			Integer hoursUntilDeadline) {
	}
}
