package com.mealbridge.domain.mealsession.dto;

import com.mealbridge.domain.mealsession.entity.TrackingMode;
import com.mealbridge.domain.mealsession.entity.TrackingSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ConfirmTrackingEventReq(
		@NotNull TrackingMode mode,
		@NotNull TrackingSource source,
		@DecimalMin("0") BigDecimal deltaKg,
		@Valid List<LineItemSnapshot> lineItems,
		String aiRawResponse
) {
}
