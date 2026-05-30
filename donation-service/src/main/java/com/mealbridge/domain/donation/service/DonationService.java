package com.mealbridge.domain.donation.service;

import com.mealbridge.domain.donation.dto.CreateDonationReq;
import com.mealbridge.domain.donation.dto.DonationResDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DonationService {

	DonationResDto create(
			UUID actorUserId,
			CreateDonationReq request,
			MultipartFile beforeImage,
			MultipartFile afterImage);

	List<DonationResDto> listMine(UUID actorUserId);

	List<DonationResDto> listAvailableForOrganization();

	DonationResDto getById(UUID id, UUID actorUserId, String role);

	void cancel(UUID id, UUID actorUserId);
}
