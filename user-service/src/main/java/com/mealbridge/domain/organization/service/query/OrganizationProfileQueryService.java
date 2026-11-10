package com.mealbridge.domain.organization.service.query;

import com.mealbridge.domain.organization.dto.OrganizationProfileResDto;

import java.util.UUID;

public interface OrganizationProfileQueryService {

	OrganizationProfileResDto getProfile(UUID userId);
}
