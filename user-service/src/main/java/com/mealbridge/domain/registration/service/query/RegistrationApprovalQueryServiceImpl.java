package com.mealbridge.domain.registration.service.query;

import com.mealbridge.domain.donor.entity.DonorInstitution;
import com.mealbridge.domain.donor.repository.DonorInstitutionRepository;
import com.mealbridge.domain.organization.entity.Organization;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.registration.dto.PendingRegistrationDetailResDto;
import com.mealbridge.domain.registration.dto.PendingRegistrationSummaryResDto;
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
@Transactional(readOnly = true)
public class RegistrationApprovalQueryServiceImpl implements RegistrationApprovalQueryService {

	private static final List<Role> REGISTRATION_ROLES = List.of(Role.ORGANIZATION, Role.DONOR);

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;

	@Override
	public List<PendingRegistrationSummaryResDto> listPending() {
		return userRepository.findByStatusAndRoleIn(UserStatus.PENDING, REGISTRATION_ROLES).stream()
				.map(this::toSummary)
				.toList();
	}

	@Override
	public PendingRegistrationDetailResDto getDetail(UUID userId) {
		return getDetailByUserId(findPendingRegistrationUser(userId).getId());
	}

	@Override
	public PendingRegistrationDetailResDto getDetailByUserId(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
		if (!REGISTRATION_ROLES.contains(user.getRole())) {
			throw new IllegalArgumentException("User is not a registration applicant");
		}
		return toDetail(user);
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

	private PendingRegistrationSummaryResDto toSummary(User user) {
		return new PendingRegistrationSummaryResDto(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				resolveInstitutionName(user),
				user.getCreatedAt()
		);
	}

	private PendingRegistrationDetailResDto toDetail(User user) {
		PendingRegistrationDetailResDto.OrganizationDetail organization = null;
		PendingRegistrationDetailResDto.DonorDetail donor = null;

		if (user.getRole() == Role.ORGANIZATION && user.getOrganizationId() != null) {
			Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
			if (org != null) {
				organization = new PendingRegistrationDetailResDto.OrganizationDetail(
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
				donor = new PendingRegistrationDetailResDto.DonorDetail(
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

		return new PendingRegistrationDetailResDto(
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
