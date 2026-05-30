package com.mealbridge.admin.service;

import com.mealbridge.admin.dto.PendingRegistrationDetail;
import com.mealbridge.admin.dto.PendingRegistrationSummary;
import com.mealbridge.domain.common.InstitutionStatus;
import com.mealbridge.domain.donor.DonorInstitution;
import com.mealbridge.domain.donor.DonorInstitutionRepository;
import com.mealbridge.domain.organization.Organization;
import com.mealbridge.domain.organization.OrganizationRepository;
import com.mealbridge.domain.user.Role;
import com.mealbridge.domain.user.User;
import com.mealbridge.domain.user.UserRepository;
import com.mealbridge.domain.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationApprovalService {

	private static final List<Role> REGISTRATION_ROLES = List.of(Role.ORGANIZATION, Role.DONOR);

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;

	@Transactional(readOnly = true)
	public List<PendingRegistrationSummary> listPending() {
		return userRepository.findByStatusAndRoleIn(UserStatus.PENDING, REGISTRATION_ROLES).stream()
				.map(this::toSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public PendingRegistrationDetail getDetail(UUID userId) {
		User user = findPendingRegistrationUser(userId);
		return toDetail(user);
	}

	@Transactional
	public PendingRegistrationDetail approve(UUID userId) {
		User user = findPendingRegistrationUser(userId);
		user.updateStatus(UserStatus.ACTIVE);
		activateInstitution(user);
		return toDetail(user);
	}

	@Transactional
	public PendingRegistrationDetail reject(UUID userId, String reason) {
		User user = findPendingRegistrationUser(userId);
		user.updateStatus(UserStatus.REJECTED);
		if (reason != null && !reason.isBlank()) {
			user.updateMemo(reason);
		}
		suspendInstitution(user);
		return toDetail(user);
	}

	private User findPendingRegistrationUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (!REGISTRATION_ROLES.contains(user.getRole())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a registration applicant");
		}
		if (user.getStatus() != UserStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not pending approval");
		}
		return user;
	}

	private void activateInstitution(User user) {
		if (user.getRole() == Role.ORGANIZATION && user.getOrganizationId() != null) {
			Organization org = organizationRepository.findById(user.getOrganizationId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
			org.updateStatus(InstitutionStatus.ACTIVE);
		}
		if (user.getRole() == Role.DONOR && user.getDonorInstitutionId() != null) {
			DonorInstitution donor = donorInstitutionRepository.findById(user.getDonorInstitutionId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor institution not found"));
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

	private PendingRegistrationSummary toSummary(User user) {
		String institutionName = resolveInstitutionName(user);
		return new PendingRegistrationSummary(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				institutionName,
				user.getCreatedAt()
		);
	}

	private PendingRegistrationDetail toDetail(User user) {
		PendingRegistrationDetail.OrganizationDetail organization = null;
		PendingRegistrationDetail.DonorDetail donor = null;

		if (user.getRole() == Role.ORGANIZATION && user.getOrganizationId() != null) {
			Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
			if (org != null) {
				organization = new PendingRegistrationDetail.OrganizationDetail(
						org.getId(),
						org.getName(),
						org.getType(),
						org.getAddress(),
						org.getOrganizationPhone(),
						org.getServiceArea(),
						org.getRecipientCapacity(),
						org.getTargetDemographic(),
						org.getRegistrationNumber()
				);
			}
		}

		if (user.getRole() == Role.DONOR && user.getDonorInstitutionId() != null) {
			DonorInstitution institution = donorInstitutionRepository.findById(user.getDonorInstitutionId()).orElse(null);
			if (institution != null) {
				donor = new PendingRegistrationDetail.DonorDetail(
						institution.getId(),
						institution.getName(),
						institution.getType(),
						institution.getAddress(),
						institution.getInstitutionPhone(),
						institution.getDescription(),
						institution.getBusinessRegistrationNumber(),
						institution.getUnitIdentifier()
				);
			}
		}

		return new PendingRegistrationDetail(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				organization,
				donor
		);
	}

	private String resolveInstitutionName(User user) {
		if (user.getRole() == Role.ORGANIZATION && user.getOrganizationId() != null) {
			return organizationRepository.findById(user.getOrganizationId())
					.map(Organization::getName)
					.orElse(null);
		}
		if (user.getRole() == Role.DONOR && user.getDonorInstitutionId() != null) {
			return donorInstitutionRepository.findById(user.getDonorInstitutionId())
					.map(DonorInstitution::getName)
					.orElse(null);
		}
		return null;
	}
}
