package com.mealbridge.domain.donation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AcceptDonationReq(@NotNull UUID organizationId) {
}
