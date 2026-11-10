package com.mealbridge.domain.mealsession.repository;

import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealsession.entity.TrackingEvent;
import com.mealbridge.domain.mealsession.entity.TrackingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {

	List<TrackingEvent> findTop20BySessionIdOrderByCreatedAtDesc(UUID sessionId);

	List<TrackingEvent> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

	List<TrackingEvent> findBySessionIdAndMode(UUID sessionId, TrackingMode mode);

	@Query("""
			SELECT e FROM TrackingEvent e, MealSession s
			WHERE e.sessionId = s.id
			AND s.donorInstitutionId = :donorInstitutionId
			AND s.sessionDate >= :from AND s.sessionDate <= :to
			AND e.mode = :mode
			AND (:mealType IS NULL OR s.mealType = :mealType)
			ORDER BY e.createdAt ASC
			""")
	List<TrackingEvent> findForStatistics(
			@Param("donorInstitutionId") UUID donorInstitutionId,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to,
			@Param("mode") TrackingMode mode,
			@Param("mealType") MealType mealType);

	@Query("""
			SELECT e FROM TrackingEvent e, MealSession s
			WHERE e.sessionId = s.id
			AND s.donorInstitutionId = :donorInstitutionId
			AND s.sessionDate >= :from AND s.sessionDate <= :to
			ORDER BY e.createdAt ASC
			""")
	List<TrackingEvent> findAllForStatistics(
			@Param("donorInstitutionId") UUID donorInstitutionId,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to);

	void deleteBySessionIdIn(Collection<UUID> sessionIds);
}
