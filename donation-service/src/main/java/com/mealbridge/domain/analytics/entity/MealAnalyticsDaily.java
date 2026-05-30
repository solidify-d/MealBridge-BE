package com.mealbridge.domain.analytics.entity;

import com.mealbridge.common.domain.BaseIdAndTimeEntity;
import com.mealbridge.domain.donation.entity.MealType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
		name = "meal_analytics_daily",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_meal_analytics_daily",
				columnNames = { "donor_institution_id", "session_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealAnalyticsDaily extends BaseIdAndTimeEntity {

	@Column(name = "donor_institution_id", nullable = false)
	private UUID donorInstitutionId;

	@Column(name = "session_id", nullable = false)
	private UUID sessionId;

	@Column(name = "session_date", nullable = false)
	private LocalDate sessionDate;

	@Column(name = "day_of_week", nullable = false)
	private int dayOfWeek;

	@Enumerated(EnumType.STRING)
	@Column(name = "meal_type", nullable = false, length = 32)
	private MealType mealType;

	@Column(name = "menu_fingerprint", nullable = false, length = 500)
	private String menuFingerprint;

	@Column(name = "headcount")
	private Integer headcount;

	@Column(name = "prepared_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal preparedKg;

	@Column(name = "served_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal servedKg;

	@Column(name = "wasted_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal wastedKg;

	@Column(name = "weather_code")
	private Integer weatherCode;

	@Column(name = "temperature_c")
	private Double temperatureC;

	@Column(name = "humidity_pct")
	private Integer humidityPct;

	@Builder
	public MealAnalyticsDaily(
			UUID donorInstitutionId,
			UUID sessionId,
			LocalDate sessionDate,
			int dayOfWeek,
			MealType mealType,
			String menuFingerprint,
			Integer headcount,
			BigDecimal preparedKg,
			BigDecimal servedKg,
			BigDecimal wastedKg,
			Integer weatherCode,
			Double temperatureC,
			Integer humidityPct) {
		this.donorInstitutionId = donorInstitutionId;
		this.sessionId = sessionId;
		this.sessionDate = sessionDate;
		this.dayOfWeek = dayOfWeek;
		this.mealType = mealType;
		this.menuFingerprint = menuFingerprint;
		this.headcount = headcount;
		this.preparedKg = preparedKg;
		this.servedKg = servedKg;
		this.wastedKg = wastedKg;
		this.weatherCode = weatherCode;
		this.temperatureC = temperatureC;
		this.humidityPct = humidityPct;
	}

	public void updateMetrics(
			Integer headcount,
			BigDecimal preparedKg,
			BigDecimal servedKg,
			BigDecimal wastedKg,
			Integer weatherCode,
			Double temperatureC,
			Integer humidityPct,
			String menuFingerprint) {
		this.headcount = headcount;
		this.preparedKg = preparedKg;
		this.servedKg = servedKg;
		this.wastedKg = wastedKg;
		this.weatherCode = weatherCode;
		this.temperatureC = temperatureC;
		this.humidityPct = humidityPct;
		this.menuFingerprint = menuFingerprint;
	}
}
