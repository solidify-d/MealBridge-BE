package com.mealbridge.domain.donor.dto;

import com.mealbridge.domain.donor.entity.PickupAcceptanceMode;

import java.util.UUID;

public record DonorInstitutionSettingsResDto(
		UUID id,
		PickupAcceptanceMode pickupAcceptanceMode) {
}
