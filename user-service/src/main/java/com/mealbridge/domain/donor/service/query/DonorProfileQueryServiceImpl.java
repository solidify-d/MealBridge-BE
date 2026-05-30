package com.mealbridge.domain.donor.service.query;

import com.mealbridge.common.profileimage.ProfileImageUrlCache;
import com.mealbridge.domain.donor.dto.DonorProfileResDto;
import com.mealbridge.domain.donor.repository.DonorInstitutionRepository;
import com.mealbridge.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonorProfileQueryServiceImpl implements DonorProfileQueryService {

	private final UserRepository userRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;
	private final ProfileImageUrlCache profileImageUrlCache;

	@Override
	public DonorProfileResDto getProfile(UUID userId) {
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
		if (user.getDonorInstitutionId() == null) {
			throw new EntityNotFoundException("Donor institution not linked to user");
		}
		var institution = donorInstitutionRepository.findById(user.getDonorInstitutionId())
				.orElseThrow(() -> new EntityNotFoundException("Donor institution not found"));
		return new DonorProfileResDto(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getStatus(),
				profileImageUrlCache.getUrl(user.getId()),
				institution.getId(),
				institution.getName(),
				institution.getType(),
				institution.getAddress(),
				institution.getInstitutionPhone(),
				institution.getStatus()
		);
	}
}
