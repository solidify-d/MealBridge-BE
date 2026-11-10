package com.mealbridge.domain.organization.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.domain.common.entity.InstitutionStatus;
import com.mealbridge.domain.organization.dto.ActiveOrganizationResDto;
import com.mealbridge.domain.organization.dto.OrganizationContactResDto;
import com.mealbridge.domain.organization.entity.Organization;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/organizations")
@RequiredArgsConstructor
public class InternalOrganizationController {

	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;

	@GetMapping("/active")
	public ResponseEntity<?> listActive() {
		List<ActiveOrganizationResDto> organizations = organizationRepository
				.findByStatus(InstitutionStatus.ACTIVE).stream()
				.map(this::toDto)
				.toList();
		return ApiResponse.ok(organizations, "Active organizations retrieved");
	}

	@GetMapping("/{id}/contact")
	public ResponseEntity<?> contact(@PathVariable UUID id) {
		Organization organization = organizationRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Organization not found"));
		User contactUser = userRepository.findFirstByOrganizationId(organization.getId()).orElse(null);
		return ApiResponse.ok(
				new OrganizationContactResDto(
						organization.getId(),
						organization.getName(),
						organization.getOrganizationPhone(),
						contactUser != null ? contactUser.getName() : null,
						contactUser != null ? contactUser.getPhone() : null),
				"Organization contact retrieved");
	}

	private ActiveOrganizationResDto toDto(Organization organization) {
		return new ActiveOrganizationResDto(
				organization.getId(),
				organization.getName(),
				organization.getAddress(),
				organization.getLatitude(),
				organization.getLongitude(),
				organization.getRecipientCapacity(),
				organization.getServiceArea(),
				organization.getTargetDemographic() != null
						? organization.getTargetDemographic().name()
						: null,
				organization.getMaxPickupDistanceKm(),
				organization.getDesiredPickupWeightKg(),
				organization.getServingStartTime() != null
						? organization.getServingStartTime().toString()
						: null);
	}
}
