package com.mealbridge.domain.donor.service.query;

import com.mealbridge.domain.donor.dto.DonorProfileResDto;

import java.util.UUID;

public interface DonorProfileQueryService {

	DonorProfileResDto getProfile(UUID userId);
}
