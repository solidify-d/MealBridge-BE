package com.mealbridge.domain.donor.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.domain.donor.dto.DonorInstitutionLocationResDto;
import com.mealbridge.domain.donor.dto.DonorInstitutionSettingsResDto;
import com.mealbridge.domain.donor.entity.DonorInstitution;
import com.mealbridge.domain.donor.repository.DonorInstitutionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/donor-institutions")
@RequiredArgsConstructor
public class InternalDonorInstitutionController {

	private final DonorInstitutionRepository donorInstitutionRepository;

	@GetMapping("/{id}/location")
	public ResponseEntity<?> location(@PathVariable UUID id) {
		DonorInstitution institution = donorInstitutionRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Donor institution not found"));
		return ApiResponse.ok(
				new DonorInstitutionLocationResDto(
						institution.getId(),
						institution.getName(),
						institution.getAddress(),
						institution.getLatitude(),
						institution.getLongitude()),
				"Donor institution location");
	}

	@GetMapping("/{id}/settings")
	public ResponseEntity<?> settings(@PathVariable UUID id) {
		DonorInstitution institution = donorInstitutionRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Donor institution not found"));
		return ApiResponse.ok(
				new DonorInstitutionSettingsResDto(institution.getId(), institution.getPickupAcceptanceMode()),
				"Donor institution settings");
	}
}
