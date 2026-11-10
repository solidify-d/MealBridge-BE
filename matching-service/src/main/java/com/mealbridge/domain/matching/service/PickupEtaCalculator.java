package com.mealbridge.domain.matching.service;

import com.mealbridge.common.util.KstDates;
import com.mealbridge.domain.matching.dto.RouteStopResDto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PickupEtaCalculator {

	private static final long STOP_BUFFER_SECONDS = 600;

	private PickupEtaCalculator() {
	}

	public static Map<UUID, LocalDateTime> estimateArrivals(List<RouteStopResDto> stops, LocalDateTime departAt) {
		Map<UUID, LocalDateTime> arrivals = new HashMap<>();
		LocalDateTime cursor = departAt;
		for (RouteStopResDto stop : stops) {
			cursor = cursor.plusSeconds(stop.durationFromPreviousS() + STOP_BUFFER_SECONDS);
			arrivals.put(stop.donationId(), cursor);
		}
		return arrivals;
	}

	public static Map<UUID, LocalDateTime> estimateArrivalsNow(List<RouteStopResDto> stops) {
		return estimateArrivals(stops, KstDates.now());
	}
}
