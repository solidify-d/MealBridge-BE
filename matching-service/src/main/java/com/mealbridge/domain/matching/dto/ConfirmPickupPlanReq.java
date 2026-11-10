package com.mealbridge.domain.matching.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ConfirmPickupPlanReq(
		@NotNull UUID organizationId,
		@NotEmpty List<UUID> donationIds) {
}
