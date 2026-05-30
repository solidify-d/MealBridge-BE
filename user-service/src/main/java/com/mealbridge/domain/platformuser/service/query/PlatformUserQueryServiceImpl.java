package com.mealbridge.domain.platformuser.service.query;

import com.mealbridge.domain.donor.entity.DonorInstitution;
import com.mealbridge.domain.donor.repository.DonorInstitutionRepository;
import com.mealbridge.domain.organization.entity.Organization;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.platformuser.dto.PlatformUserDetailResDto;
import com.mealbridge.domain.platformuser.dto.PlatformUserSummaryResDto;
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
@Transactional(readOnly = true)
public class PlatformUserQueryServiceImpl implements PlatformUserQueryService {

	private static final List<Role> REGISTRATION_ROLES = List.of(Role.ORGANIZATION, Role.DONOR);

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;
	private final RegistrationApprovalQueryService registrationApprovalQueryService;

	@Override
	public List<PlatformUserSummaryResDto> listAll(Role role, UserStatus status) {
		return userRepository.findAllByOrderByCreatedAtDesc().stream()
				.filter(user -> role == null || user.getRole() == role)
				.filter(user -> status == null || user.getStatus() == status)
				.map(this::toSummary)
				.toList();
	}

	@Override
	public PlatformUserDetailResDto getDetail(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		if (REGISTRATION_ROLES.contains(user.getRole())) {
			PendingRegistrationDetailResDto registration =
					registrationApprovalQueryService.getDetailByUserId(userId);
			return new PlatformUserDetailResDto(
					registration.userId(),
					registration.email(),
					registration.contactName(),
					registration.contactPhone(),
					registration.role(),
					registration.status(),
					user.getSuspendReason(),
					user.getCreatedAt(),
					null,
					null,
					registration.organization(),
					registration.donor());
		}

		return new PlatformUserDetailResDto(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				user.getSuspendReason(),
				user.getCreatedAt(),
				user.getDepartment(),
				user.getMemo(),
				null,
				null);
	}

	private PlatformUserSummaryResDto toSummary(User user) {
		return new PlatformUserSummaryResDto(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				resolveInstitutionName(user),
				user.getCreatedAt());
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
