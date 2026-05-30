package com.mealbridge.domain.donation.service;

import com.mealbridge.common.s3.DonationImageStorage;
import com.mealbridge.common.util.JsonUtil;
import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.donation.dto.AcceptDonationReq;
import com.mealbridge.domain.donation.dto.CreateDonationReq;
import com.mealbridge.domain.donation.dto.DonationResDto;
import com.mealbridge.domain.donation.dto.ManualDonationReq;
import com.mealbridge.domain.donation.entity.Donation;
import com.mealbridge.domain.donation.entity.DonationStatus;
import com.mealbridge.domain.donation.repository.DonationRepository;
import com.mealbridge.domain.esg.service.EsgAggregationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DonationServiceImpl implements DonationService {

	private static final int DEFAULT_PICKUP_HOURS = 4;

	private final DonationRepository donationRepository;
	private final DonationImageStorage donationImageStorage;
	private final EsgAggregationService esgAggregationService;

	@Override
	public DonationResDto create(
			UUID actorUserId,
			CreateDonationReq request,
			MultipartFile beforeImage,
			MultipartFile afterImage) {
		expireStaleDonations();
		if (afterImage == null || afterImage.isEmpty()) {
			throw new IllegalArgumentException("After-meal image is required");
		}

		Donation donation = Donation.builder()
				.donorInstitutionId(request.donorInstitutionId())
				.registeredByUserId(actorUserId)
				.mealType(request.mealType())
				.menuName(request.menuName().trim())
				.servingEndedAt(request.servingEndedAt())
				.storageStartedAt(request.storageStartedAt())
				.pickupDeadlineAt(resolvePickupDeadline(request.pickupDeadlineAt()))
				.notes(request.notes())
				.mealSessionId(null)
				.totalWeightKg(null)
				.lineItemsJson(null)
				.build();
		donationRepository.save(donation);

		String beforeUrl = null;
		String afterUrl = null;
		try {
			if (beforeImage != null && !beforeImage.isEmpty()) {
				beforeUrl = donationImageStorage.upload(
						donation.getDonorInstitutionId(), donation.getId(), beforeImage);
			}
			afterUrl = donationImageStorage.upload(
					donation.getDonorInstitutionId(), donation.getId(), afterImage);
			donation.updateImages(beforeUrl, afterUrl);
			return toDto(donation);
		} catch (RuntimeException e) {
			donationImageStorage.deleteQuietly(beforeUrl);
			donationImageStorage.deleteQuietly(afterUrl);
			donationRepository.delete(donation);
			throw e;
		}
	}

	@Override
	public DonationResDto createManual(
			UUID actorUserId,
			ManualDonationReq request,
			MultipartFile beforeImage,
			MultipartFile afterImage) {
		expireStaleDonations();
		String lineItemsJson = JsonUtil.toJson(request.lineItems());
		Donation donation = Donation.builder()
				.donorInstitutionId(request.donorInstitutionId())
				.registeredByUserId(actorUserId)
				.mealType(request.mealType())
				.menuName(request.menuName().trim())
				.servingEndedAt(request.servingEndedAt())
				.storageStartedAt(request.storageStartedAt())
				.pickupDeadlineAt(resolvePickupDeadline(request.pickupDeadlineAt()))
				.notes(request.notes())
				.mealSessionId(request.mealSessionId())
				.totalWeightKg(request.totalWeightKg())
				.lineItemsJson(lineItemsJson)
				.build();
		donationRepository.save(donation);

		String beforeUrl = null;
		String afterUrl = null;
		try {
			if (beforeImage != null && !beforeImage.isEmpty()) {
				beforeUrl = donationImageStorage.upload(
						donation.getDonorInstitutionId(), donation.getId(), beforeImage);
			}
			if (afterImage != null && !afterImage.isEmpty()) {
				afterUrl = donationImageStorage.upload(
						donation.getDonorInstitutionId(), donation.getId(), afterImage);
			}
			if (beforeUrl != null || afterUrl != null) {
				donation.updateImages(beforeUrl, afterUrl);
			}
			return toDto(donation);
		} catch (RuntimeException e) {
			donationImageStorage.deleteQuietly(beforeUrl);
			donationImageStorage.deleteQuietly(afterUrl);
			donationRepository.delete(donation);
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<DonationResDto> listMine(UUID actorUserId) {
		return donationRepository.findByRegisteredByUserIdOrderByCreatedAtDesc(actorUserId).stream()
				.map(this::toDto)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DonationResDto> listAvailableForOrganization() {
		LocalDateTime now = KstDates.now();
		return donationRepository.findByStatusOrderByCreatedAtDesc(DonationStatus.REGISTERED).stream()
				.filter(donation -> donation.isPickupAvailable(now))
				.map(this::toDto)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public DonationResDto getById(UUID id, UUID actorUserId, String role) {
		Donation donation = requireDonation(id);
		if ("DONOR".equalsIgnoreCase(role)) {
			if (!donation.getRegisteredByUserId().equals(actorUserId)) {
				throw new SecurityException("Access denied");
			}
		} else if ("ORGANIZATION".equalsIgnoreCase(role)) {
			if (!donation.isPickupAvailable(KstDates.now())) {
				throw new SecurityException("Donation is not available");
			}
		} else {
			throw new SecurityException("Access denied");
		}
		return toDto(donation);
	}

	@Override
	public int expireStaleDonations() {
		LocalDateTime now = KstDates.now();
		List<Donation> stale = donationRepository.findByStatusAndPickupDeadlineAtBefore(
				DonationStatus.REGISTERED, now);
		stale.forEach(Donation::expire);
		return stale.size();
	}

	@Override
	public DonationResDto accept(UUID id, UUID actorUserId, AcceptDonationReq request) {
		Donation donation = requireDonation(id);
		if (!donation.isPickupAvailable(KstDates.now())) {
			throw new IllegalStateException("Donation is not available for acceptance");
		}
		donation.accept(request.organizationId(), KstDates.now());
		java.math.BigDecimal weightKg = donation.getTotalWeightKg();
		if (weightKg != null && weightKg.signum() > 0) {
			esgAggregationService.recordSurplusDonated(
					donation.getDonorInstitutionId(), donation.getId(), weightKg);
			esgAggregationService.recordSurplusReceived(
					request.organizationId(), donation.getId(), weightKg);
		}
		return toDto(donation);
	}

	@Override
	public DonationResDto markInTransit(UUID id, UUID actorUserId, UUID organizationId) {
		Donation donation = requireDonation(id);
		assertAcceptedOrganization(donation, organizationId);
		donation.markInTransit(KstDates.now());
		return toDto(donation);
	}

	@Override
	public DonationResDto complete(UUID id, UUID actorUserId, UUID organizationId) {
		Donation donation = requireDonation(id);
		assertAcceptedOrganization(donation, organizationId);
		donation.complete(KstDates.now());
		return toDto(donation);
	}

	@Override
	public DonationResDto release(UUID id, UUID actorUserId, UUID organizationId) {
		Donation donation = requireDonation(id);
		assertAcceptedOrganization(donation, organizationId);
		donation.releaseFromTransit();
		return toDto(donation);
	}

	@Override
	public void cancel(UUID id, UUID actorUserId) {
		Donation donation = requireDonation(id);
		if (!donation.getRegisteredByUserId().equals(actorUserId)) {
			throw new SecurityException("Access denied");
		}
		donation.cancel();
		donationImageStorage.deleteQuietly(donation.getBeforeImageUrl());
		donationImageStorage.deleteQuietly(donation.getAfterImageUrl());
		donation.clearImages();
	}

	@Override
	@Transactional(readOnly = true)
	public DonationResDto getInternalById(UUID id) {
		return toDto(requireDonation(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DonationResDto> listRegisteredInternal() {
		LocalDateTime now = KstDates.now();
		return donationRepository.findByStatusOrderByCreatedAtDesc(DonationStatus.REGISTERED).stream()
				.filter(donation -> donation.isPickupAvailable(now))
				.map(this::toDto)
				.toList();
	}

	private Donation requireDonation(UUID id) {
		return donationRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Donation not found"));
	}

	private void assertAcceptedOrganization(Donation donation, UUID organizationId) {
		if (organizationId == null) {
			throw new IllegalArgumentException("organizationId is required");
		}
		if (donation.getAcceptedOrganizationId() == null
				|| !donation.getAcceptedOrganizationId().equals(organizationId)) {
			throw new SecurityException("Organization is not assigned to this donation");
		}
	}

	private LocalDateTime resolvePickupDeadline(LocalDateTime requested) {
		LocalDateTime deadline = requested != null ? requested : KstDates.now().plusHours(DEFAULT_PICKUP_HOURS);
		if (deadline.isBefore(KstDates.now())) {
			throw new IllegalArgumentException("Pickup deadline must be in the future");
		}
		return deadline;
	}

	private DonationResDto toDto(Donation donation) {
		return new DonationResDto(
				donation.getId(),
				donation.getDonorInstitutionId(),
				donation.getRegisteredByUserId(),
				donation.getStatus(),
				donation.getMealType(),
				donation.getMenuName(),
				donation.getServingEndedAt(),
				donation.getStorageStartedAt(),
				donation.getPickupDeadlineAt(),
				donation.getBeforeImageUrl(),
				donation.getAfterImageUrl(),
				donation.getNotes(),
				donation.getMealSessionId(),
				donation.getTotalWeightKg(),
				donation.getLineItemsJson(),
				donation.getAcceptedOrganizationId(),
				donation.getAcceptedAt(),
				resolveInTransitAt(donation),
				resolveCompletedAt(donation),
				donation.getCreatedAt(),
				donation.getUpdatedAt());
	}

	private LocalDateTime resolveInTransitAt(Donation donation) {
		if (donation.getInTransitAt() != null) {
			return donation.getInTransitAt();
		}
		if (donation.getStatus() == DonationStatus.IN_TRANSIT
				|| donation.getStatus() == DonationStatus.COMPLETED) {
			return donation.getUpdatedAt();
		}
		return null;
	}

	private LocalDateTime resolveCompletedAt(Donation donation) {
		if (donation.getCompletedAt() != null) {
			return donation.getCompletedAt();
		}
		if (donation.getStatus() == DonationStatus.COMPLETED) {
			return donation.getUpdatedAt();
		}
		return null;
	}
}
