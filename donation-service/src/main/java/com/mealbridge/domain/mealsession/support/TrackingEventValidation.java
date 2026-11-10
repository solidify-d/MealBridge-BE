package com.mealbridge.domain.mealsession.support;

import com.mealbridge.domain.mealsession.dto.ConfirmTrackingEventReq;
import com.mealbridge.domain.mealsession.dto.LineItemSnapshot;

import java.math.BigDecimal;
import java.util.List;

public final class TrackingEventValidation {

	private TrackingEventValidation() {
	}

	public static boolean hasMeaningfulPayload(ConfirmTrackingEventReq request) {
		if (request.deltaKg() != null && request.deltaKg().signum() > 0) {
			return true;
		}
		List<LineItemSnapshot> items = request.lineItems();
		if (items == null || items.isEmpty()) {
			return false;
		}
		return items.stream()
				.anyMatch(item -> item.kg() != null && item.kg().signum() > 0);
	}

	public static BigDecimal resolveDeltaKg(ConfirmTrackingEventReq request) {
		if (request.deltaKg() != null && request.deltaKg().signum() > 0) {
			return request.deltaKg();
		}
		if (request.lineItems() == null) {
			return BigDecimal.ZERO;
		}
		return request.lineItems().stream()
				.map(LineItemSnapshot::kg)
				.filter(kg -> kg != null && kg.signum() > 0)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
