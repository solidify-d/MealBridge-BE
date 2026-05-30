package com.mealbridge.domain.donation.service;

import com.mealbridge.common.s3.DonationImageStorage;
import com.mealbridge.domain.donation.dto.CreateDonationReq;
import com.mealbridge.domain.donation.dto.DonationResDto;
import com.mealbridge.domain.donation.entity.Donation;
import com.mealbridge.domain.donation.entity.DonationStatus;
import com.mealbridge.domain.donation.repository.DonationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DonationServiceImpl implements DonationService {

	private final DonationRepository donationRepository;
	private final DonationImageStorage donationImageStorage;

	@Override
	public DonationResDto create(
			UUID actorUserId,
			CreateDonationReq request,
			MultipartFile beforeImage,
			MultipartFile afterImage) {
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
				.notes(request.notes())
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
	@Transactional(readOnly = true)
	public List<DonationResDto> listMine(UUID actorUserId) {
		return donationRepository.findByRegisteredByUserIdOrderByCreatedAtDesc(actorUserId).stream()
				.map(this::toDto)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DonationResDto> listAvailableForOrganization() {
		return donationRepository.findByStatusOrderByCreatedAtDesc(DonationStatus.REGISTERED).stream()
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
			if (donation.getStatus() != DonationStatus.REGISTERED) {
				throw new SecurityException("Donation is not available");
			}
		} else {
			throw new SecurityException("Access denied");
		}
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

	private Donation requireDonation(UUID id) {
		return donationRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Donation not found"));
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
				donation.getBeforeImageUrl(),
				donation.getAfterImageUrl(),
				donation.getNotes(),
				donation.getCreatedAt(),
				donation.getUpdatedAt());
	}
}
