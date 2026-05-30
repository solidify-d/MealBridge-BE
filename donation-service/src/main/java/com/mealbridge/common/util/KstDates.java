package com.mealbridge.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class KstDates {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private KstDates() {
	}

	public static LocalDate today() {
		return LocalDate.now(ZONE);
	}

	public static LocalDateTime now() {
		return LocalDateTime.now(ZONE);
	}
}
