package com.mealbridge.domain.mealsession.entity;

import com.mealbridge.common.domain.BaseIdAndTimeEntity;
import com.mealbridge.domain.donation.entity.MealType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "meal_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealSession extends BaseIdAndTimeEntity {

	@Column(name = "donor_institution_id", nullable = false)
	private UUID donorInstitutionId;

	@Column(name = "registered_by_user_id", nullable = false)
	private UUID registeredByUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private MealSessionStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "meal_type", nullable = false, length = 32)
	private MealType mealType;

	@Column(name = "menu_name", nullable = false, length = 200)
	private String menuName;

	@Column(name = "session_date", nullable = false)
	private LocalDate sessionDate;

	@Column(name = "prepared_weight_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal preparedWeightKg;

	@Column(name = "remaining_weight_kg", nullable = false, precision = 12, scale = 3)
	private BigDecimal remainingWeightKg;

	@Column(name = "headcount")
	private Integer headcount;

	@Column(length = 1000)
	private String notes;

	@Column(name = "weather_latitude")
	private Double weatherLatitude;

	@Column(name = "weather_longitude")
	private Double weatherLongitude;

	@Column(name = "weather_temperature_c")
	private Double weatherTemperatureC;

	@Column(name = "weather_humidity_pct")
	private Integer weatherHumidityPct;

	@Column(name = "weather_precipitation_mm")
	private Double weatherPrecipitationMm;

	@Column(name = "weather_wind_speed_kmh")
	private Double weatherWindSpeedKmh;

	@Column(name = "weather_code")
	private Integer weatherCode;

	@Column(name = "weather_captured_at")
	private java.time.LocalDateTime weatherCapturedAt;

	@Column(name = "weather_snapshot_json", columnDefinition = "TEXT")
	private String weatherSnapshotJson;

	@Column(name = "expected_serving_end_at")
	private java.time.LocalDateTime expectedServingEndAt;

	@Column(name = "predicted_leftover_json", columnDefinition = "TEXT")
	private String predictedLeftoverJson;

	@Builder
	public MealSession(
			UUID donorInstitutionId,
			UUID registeredByUserId,
			MealType mealType,
			String menuName,
			LocalDate sessionDate,
			BigDecimal preparedWeightKg,
			Integer headcount,
			String notes) {
		this.donorInstitutionId = donorInstitutionId;
		this.registeredByUserId = registeredByUserId;
		this.status = MealSessionStatus.DRAFT;
		this.mealType = mealType;
		this.menuName = menuName.trim();
		this.sessionDate = sessionDate;
		this.preparedWeightKg = preparedWeightKg;
		this.remainingWeightKg = preparedWeightKg;
		this.headcount = headcount;
		this.notes = notes;
	}

	public void transitionTo(MealSessionStatus next) {
		this.status = next;
	}

	public void applyServingDelta(BigDecimal servedKg) {
		if (servedKg == null || servedKg.signum() < 0) {
			throw new IllegalArgumentException("Served amount must be non-negative");
		}
		this.remainingWeightKg = this.remainingWeightKg.subtract(servedKg).max(BigDecimal.ZERO);
	}

	public void setRemainingWeight(BigDecimal remainingKg) {
		if (remainingKg == null || remainingKg.signum() < 0) {
			throw new IllegalArgumentException("Remaining weight must be non-negative");
		}
		this.remainingWeightKg = remainingKg;
	}

	public void close() {
		this.status = MealSessionStatus.CLOSED;
	}

	public void applyWeatherSnapshot(
			Double latitude,
			Double longitude,
			Double temperatureC,
			Integer humidityPct,
			Double precipitationMm,
			Double windSpeedKmh,
			Integer weatherCode,
			java.time.LocalDateTime capturedAt,
			String snapshotJson) {
		this.weatherLatitude = latitude;
		this.weatherLongitude = longitude;
		this.weatherTemperatureC = temperatureC;
		this.weatherHumidityPct = humidityPct;
		this.weatherPrecipitationMm = precipitationMm;
		this.weatherWindSpeedKmh = windSpeedKmh;
		this.weatherCode = weatherCode;
		this.weatherCapturedAt = capturedAt;
		this.weatherSnapshotJson = snapshotJson;
	}

	public void setExpectedServingEndAt(java.time.LocalDateTime expectedServingEndAt) {
		this.expectedServingEndAt = expectedServingEndAt;
	}

	public void setPredictedLeftoverJson(String predictedLeftoverJson) {
		this.predictedLeftoverJson = predictedLeftoverJson;
	}
}
