package com.mealbridge.domain.account.service;

import com.mealbridge.common.auth.repository.RefreshTokenStore;
import com.mealbridge.common.profileimage.ProfileImageUrlCache;
import com.mealbridge.common.s3.ProfileImageStorage;
import com.mealbridge.domain.account.dto.MyAccountProfileResDto;
import com.mealbridge.domain.account.dto.ProfileImageUploadResDto;
import com.mealbridge.domain.account.dto.UpdateMyAccountReq;
import com.mealbridge.domain.donor.entity.DonorInstitution;
import com.mealbridge.domain.donor.repository.DonorInstitutionRepository;
import com.mealbridge.domain.organization.entity.Organization;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountProfileServiceImpl implements AccountProfileService {

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProfileImageStorage profileImageStorage;
	private final ProfileImageUrlCache profileImageUrlCache;
	private final RefreshTokenStore refreshTokenStore;

	@Override
	@Transactional(readOnly = true)
	public MyAccountProfileResDto getProfile(UUID userId) {
		User user = requireUser(userId);
		return toProfile(user);
	}

	@Override
	public MyAccountProfileResDto updateProfile(UUID userId, UpdateMyAccountReq request) {
		User user = requireUser(userId);
		String oldImageUrl = user.getProfileImageUrl();

		if (request.name() != null && !request.name().isBlank()) {
			user.updateContactProfile(request.name().trim(), request.phone());
		} else if (request.phone() != null) {
			user.updateContactProfile(user.getName(), request.phone());
		}

		switch (user.getRole()) {
			case ADMIN, SUPER_ADMIN -> user.updateAdminProfile(
					user.getName(),
					user.getPhone(),
					request.department(),
					request.memo());
			case ORGANIZATION -> updateOrganization(user, request);
			case DONOR -> updateDonor(user, request);
		}

		if (request.newPassword() != null && !request.newPassword().isBlank()) {
			user.updatePassword(passwordEncoder.encode(request.newPassword()));
		}

		if (Boolean.TRUE.equals(request.removeProfileImage()) && oldImageUrl != null) {
			profileImageStorage.deleteQuietly(oldImageUrl);
			user.clearProfileImageUrl();
			profileImageUrlCache.put(userId, null);
		}

		return toProfile(user);
	}

	@Override
	public ProfileImageUploadResDto uploadProfileImage(UUID userId, MultipartFile file) {
		User user = requireUser(userId);
		String oldUrl = user.getProfileImageUrl();
		String uploadedUrl = null;

		try {
			uploadedUrl = profileImageStorage.upload(user, file);
			user.updateProfileImageUrl(uploadedUrl);
			if (oldUrl != null && !oldUrl.equals(uploadedUrl)) {
				profileImageStorage.deleteQuietly(oldUrl);
			}
			profileImageUrlCache.put(userId, uploadedUrl);
			return new ProfileImageUploadResDto(uploadedUrl);
		} catch (RuntimeException e) {
			if (uploadedUrl != null) {
				profileImageStorage.deleteQuietly(uploadedUrl);
			}
			throw e;
		}
	}

	@Override
	public void deleteProfileImage(UUID userId) {
		User user = requireUser(userId);
		String oldUrl = user.getProfileImageUrl();
		if (oldUrl != null) {
			profileImageStorage.delete(oldUrl);
			user.clearProfileImageUrl();
			profileImageUrlCache.put(userId, null);
		}
	}

	@Override
	public void deleteAccount(UUID userId) {
		User user = requireUser(userId);
		if (user.getRole() == Role.SUPER_ADMIN) {
			throw new IllegalStateException("Super admin account cannot be self-deleted");
		}

		String imageUrl = user.getProfileImageUrl();
		if (imageUrl != null) {
			profileImageStorage.deleteQuietly(imageUrl);
		}

		if (user.getRole() == Role.ORGANIZATION && user.getOrganizationId() != null) {
			organizationRepository.findById(user.getOrganizationId()).ifPresent(org -> {
				org.markDeleted();
			});
		}
		if (user.getRole() == Role.DONOR && user.getDonorInstitutionId() != null) {
			donorInstitutionRepository.findById(user.getDonorInstitutionId()).ifPresent(donor -> {
				donor.markDeleted();
			});
		}

		profileImageUrlCache.evict(userId);
		refreshTokenStore.deleteRefreshToken(userId);
		userRepository.delete(user);
	}

	private void updateOrganization(User user, UpdateMyAccountReq request) {
		if (user.getOrganizationId() == null) {
			throw new EntityNotFoundException("Organization not linked to user");
		}
		Organization org = organizationRepository.findById(user.getOrganizationId())
				.orElseThrow(() -> new EntityNotFoundException("Organization not found"));
		org.updateProfile(
				request.organizationName(),
				request.address(),
				request.organizationPhone(),
				request.serviceArea(),
				request.recipientCapacity(),
				request.targetDemographic(),
				request.registrationNumber());
	}

	private void updateDonor(User user, UpdateMyAccountReq request) {
		if (user.getDonorInstitutionId() == null) {
			throw new EntityNotFoundException("Donor institution not linked to user");
		}
		DonorInstitution institution = donorInstitutionRepository.findById(user.getDonorInstitutionId())
				.orElseThrow(() -> new EntityNotFoundException("Donor institution not found"));
		institution.updateProfile(
				request.institutionName(),
				request.address() != null ? request.address() : institution.getAddress(),
				request.institutionPhone(),
				request.description(),
				request.businessRegistrationNumber(),
				request.unitIdentifier());
	}

	private MyAccountProfileResDto toProfile(User user) {
		Organization org = null;
		DonorInstitution donor = null;

		if (user.getRole() == Role.ORGANIZATION && user.getOrganizationId() != null) {
			org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
		}
		if (user.getRole() == Role.DONOR && user.getDonorInstitutionId() != null) {
			donor = donorInstitutionRepository.findById(user.getDonorInstitutionId()).orElse(null);
		}

		return new MyAccountProfileResDto(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				profileImageUrlCache.getUrl(user.getId()),
				user.getDepartment(),
				user.getMemo(),
				org != null ? org.getId() : null,
				org != null ? org.getName() : null,
				org != null ? org.getType() : null,
				org != null ? org.getAddress() : (donor != null ? donor.getAddress() : null),
				org != null ? org.getOrganizationPhone() : null,
				org != null ? org.getServiceArea() : null,
				org != null ? org.getRecipientCapacity() : null,
				org != null ? org.getTargetDemographic() : null,
				org != null ? org.getRegistrationNumber() : null,
				org != null ? org.getStatus() : null,
				donor != null ? donor.getId() : null,
				donor != null ? donor.getName() : null,
				donor != null ? donor.getType() : null,
				donor != null ? donor.getInstitutionPhone() : null,
				donor != null ? donor.getDescription() : null,
				donor != null ? donor.getBusinessRegistrationNumber() : null,
				donor != null ? donor.getUnitIdentifier() : null,
				donor != null ? donor.getStatus() : null);
	}

	private User requireUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
	}
}
