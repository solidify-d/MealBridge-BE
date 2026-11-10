package com.mealbridge.domain.organization.dto;

import java.util.UUID;

public record OrganizationContactResDto(
		UUID organizationId,
		String organizationName,
		String organizationPhone,
		String contactPersonName,
		String contactPersonPhone) {
}
