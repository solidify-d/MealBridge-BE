package com.mealbridge.domain.donor.dto;

import java.util.UUID;

public record DonorInstitutionLocationResDto(
		UUID id,
		String name,
		String address,
		Double latitude,
		Double longitude
) {
}
