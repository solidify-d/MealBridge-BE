package com.mealbridge.domain.matching.dto;

import java.util.UUID;

public record CancelInTransitReq(UUID organizationId, String reason) {
}
