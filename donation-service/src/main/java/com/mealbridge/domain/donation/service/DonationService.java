package com.mealbridge.domain.donation.service;

import com.mealbridge.domain.donation.dto.AcceptDonationReq;
import com.mealbridge.domain.donation.dto.CreateDonationReq;
import com.mealbridge.domain.donation.dto.DonationResDto;
import com.mealbridge.domain.donation.dto.ManualDonationReq;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DonationService {

	DonationResDto create(
			UUID actorUserId,
			CreateDonationReq request,
			MultipartFile beforeImage,
			MultipartFile afterImage);

	DonationResDto createManual(
			UUID actorUserId,
			ManualDonationReq request,
			MultipartFile beforeImage,
			MultipartFile afterImage);

	List<DonationResDto> listMine(UUID actorUserId);

	List<DonationResDto> listAvailableForOrganization();

	DonationResDto getById(UUID id, UUID actorUserId, String role);

	void cancel(UUID id, UUID actorUserId);

	DonationResDto accept(UUID id, UUID actorUserId, AcceptDonationReq request);

	DonationResDto markInTransit(UUID id, UUID actorUserId, UUID organizationId);

	DonationResDto complete(UUID id, UUID actorUserId, UUID organizationId);

	DonationResDto release(UUID id, UUID actorUserId, UUID organizationId);

	DonationResDto getInternalById(UUID id);

	List<DonationResDto> listRegisteredInternal();

	int expireStaleDonations();
}
