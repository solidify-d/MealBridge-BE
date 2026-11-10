package com.mealbridge.domain.registration.service.command;

import com.mealbridge.domain.common.entity.InstitutionStatus;
import com.mealbridge.domain.donor.entity.DonorInstitution;
import com.mealbridge.domain.donor.repository.DonorInstitutionRepository;
import com.mealbridge.domain.organization.entity.Organization;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.registration.dto.PendingRegistrationDetailResDto;
import com.mealbridge.domain.registration.service.query.RegistrationApprovalQueryService;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationApprovalCommandServiceImpl implements RegistrationApprovalCommandService {

	private static final List<Role> REGISTRATION_ROLES = List.of(Role.ORGANIZATION, Role.DONOR);

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;
	private final RegistrationApprovalQueryService registrationApprovalQueryService;

	@Override
	public PendingRegistrationDetailResDto approve(UUID userId) {
		User user = findPendingRegistrationUser(userId);
		user.updateStatus(UserStatus.ACTIVE);
		activateInstitution(user);
		return registrationApprovalQueryService.getDetailByUserId(userId);
	}

	@Override
	public PendingRegistrationDetailResDto reject(UUID userId, String reason) {
		User user = findPendingRegistrationUser(userId);
		user.updateStatus(UserStatus.REJECTED);
		if (reason != null && !reason.isBlank()) {
			user.updateMemo(reason);
		}
		suspendInstitution(user);
		return registrationApprovalQueryService.getDetailByUserId(userId);
	}

	private User findPendingRegistrationUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		if (!REGISTRATION_ROLES.contains(user.getRole())) {
			throw new IllegalArgumentException("User is not a registration applicant");
		}
		if (user.getStatus() != UserStatus.PENDING) {
			throw new IllegalStateException("User is not pending approval");
		}
		return user;
	}

	private void activateInstitution(User user) {
		if (user.getRole() == Role.ORGANIZATION && user.getOrganizationId() != null) {
			Organization org = organizationRepository.findById(user.getOrganizationId())
					.orElseThrow(() -> new EntityNotFoundException("Organization not found"));
			org.updateStatus(InstitutionStatus.ACTIVE);
		}
		if (user.getRole() == Role.DONOR && user.getDonorInstitutionId() != null) {
			DonorInstitution donor = donorInstitutionRepository.findById(user.getDonorInstitutionId())
					.orElseThrow(() -> new EntityNotFoundException("Donor institution not found"));
			donor.updateStatus(InstitutionStatus.ACTIVE);
		}
	}

	private void suspendInstitution(User user) {
		if (user.getRole() == Role.ORGANIZATION && user.getOrganizationId() != null) {
			organizationRepository.findById(user.getOrganizationId())
					.ifPresent(org -> org.updateStatus(InstitutionStatus.SUSPENDED));
		}
		if (user.getRole() == Role.DONOR && user.getDonorInstitutionId() != null) {
			donorInstitutionRepository.findById(user.getDonorInstitutionId())
					.ifPresent(donor -> donor.updateStatus(InstitutionStatus.SUSPENDED));
		}
	}
}
