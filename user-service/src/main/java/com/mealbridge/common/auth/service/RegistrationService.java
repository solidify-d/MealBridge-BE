package com.mealbridge.common.auth.service;

import com.mealbridge.common.auth.dto.DonorRegisterRequest;
import com.mealbridge.common.auth.dto.OrganizationRegisterRequest;
import com.mealbridge.common.auth.dto.RegisterResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RegistrationService {

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public RegisterResponse registerOrganization(OrganizationRegisterRequest request) {
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
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}
	}
}
