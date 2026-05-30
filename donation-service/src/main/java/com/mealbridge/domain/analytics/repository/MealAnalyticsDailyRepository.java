package com.mealbridge.domain.analytics.repository;

import com.mealbridge.domain.analytics.entity.MealAnalyticsDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealAnalyticsDailyRepository extends JpaRepository<MealAnalyticsDaily, UUID> {

	Optional<MealAnalyticsDaily> findBySessionId(UUID sessionId);

	List<MealAnalyticsDaily> findByDonorInstitutionIdAndSessionDateBetweenOrderBySessionDateAsc(
			UUID donorInstitutionId, LocalDate from, LocalDate to);
}
