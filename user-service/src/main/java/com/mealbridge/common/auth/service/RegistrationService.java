package com.mealbridge.common.auth.service;

import com.mealbridge.common.auth.dto.DonorRegisterRequest;
import com.mealbridge.common.auth.dto.OrganizationRegisterRequest;
import com.mealbridge.common.auth.dto.RegisterResponse;
import com.mealbridge.common.auth.email.EmailVerificationService;
import com.mealbridge.common.exception.DuplicateResourceException;
import com.mealbridge.domain.common.entity.InstitutionStatus;
import com.mealbridge.domain.donor.entity.DonorInstitution;
import com.mealbridge.domain.donor.repository.DonorInstitutionRepository;
import com.mealbridge.domain.organization.entity.Organization;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationService emailVerificationService;

	@Transactional
	public RegisterResponse registerOrganization(OrganizationRegisterRequest request) {
		emailVerificationService.requireVerifiedForRegistration(request.email());
		ensureEmailAvailable(request.email());

		Organization organization = Organization.builder()
				.name(request.organizationName())
				.type(request.organizationType())
				.address(request.address())
				.organizationPhone(request.organizationPhone())
				.serviceArea(request.serviceArea())
				.recipientCapacity(request.recipientCapacity())
				.targetDemographic(request.targetDemographic())
				.registrationNumber(request.registrationNumber())
				.status(InstitutionStatus.PENDING)
				.build();
		organizationRepository.save(organization);

		User user = User.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.name(request.name())
				.phone(request.phone())
				.role(Role.ORGANIZATION)
				.status(UserStatus.PENDING)
				.emailVerified(true)
				.organizationId(organization.getId())
				.build();
		userRepository.save(user);

		return new RegisterResponse(
				user.getId(),
				user.getStatus(),
				"Organization registration submitted. Awaiting admin approval."
		);
	}

	@Transactional
	public RegisterResponse registerDonor(DonorRegisterRequest request) {
		emailVerificationService.requireVerifiedForRegistration(request.email());
		ensureEmailAvailable(request.email());

		DonorInstitution institution = DonorInstitution.builder()
				.name(request.institutionName())
				.type(request.institutionType())
				.address(request.address())
				.institutionPhone(request.institutionPhone())
				.description(request.description())
				.businessRegistrationNumber(request.businessRegistrationNumber())
				.unitIdentifier(request.unitIdentifier())
				.status(InstitutionStatus.PENDING)
				.build();
		donorInstitutionRepository.save(institution);

		User user = User.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.name(request.name())
				.phone(request.phone())
				.role(Role.DONOR)
				.status(UserStatus.PENDING)
				.emailVerified(true)
				.donorInstitutionId(institution.getId())
				.build();
		userRepository.save(user);

		return new RegisterResponse(
				user.getId(),
				user.getStatus(),
				"Donor institution registration submitted. Awaiting admin approval."
		);
	}

	private void ensureEmailAvailable(String email) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new DuplicateResourceException("Email already exists");
		}
	}
}
