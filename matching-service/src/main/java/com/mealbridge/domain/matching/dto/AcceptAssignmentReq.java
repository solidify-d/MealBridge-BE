package com.mealbridge.domain.matching.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AcceptAssignmentReq(
		@NotNull UUID donationId,
		@NotNull UUID organizationId) {
}
