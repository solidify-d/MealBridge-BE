package com.mealbridge.domain.esg.repository;

import com.mealbridge.domain.esg.entity.EsgActorType;
import com.mealbridge.domain.esg.entity.EsgContribution;
import com.mealbridge.domain.esg.entity.EsgEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EsgContributionRepository extends JpaRepository<EsgContribution, UUID> {

	boolean existsByActorTypeAndActorIdAndEventTypeAndSourceRef(
			EsgActorType actorType,
			UUID actorId,
			EsgEventType eventType,
			UUID sourceRef);

	long countByActorTypeAndActorIdAndMonthAndEventType(
			EsgActorType actorType,
			UUID actorId,
			String month,
			EsgEventType eventType);

	@Query("""
			SELECT e.eventType, COALESCE(SUM(e.weightKg), 0), COALESCE(SUM(e.co2eKg), 0)
			FROM EsgContribution e
			WHERE e.actorType = :actorType
			AND e.actorId = :actorId
			AND e.month = :month
			GROUP BY e.eventType
			""")
	List<Object[]> aggregateByEventType(
			@Param("actorType") EsgActorType actorType,
			@Param("actorId") UUID actorId,
			@Param("month") String month);

	@Query("""
			SELECT MIN(e.month) FROM EsgContribution e
			WHERE e.actorType = :actorType AND e.actorId = :actorId
			""")
	Optional<String> findFirstMonth(
			@Param("actorType") EsgActorType actorType,
			@Param("actorId") UUID actorId);

	@Query("""
			SELECT e.eventType, COALESCE(SUM(e.weightKg), 0), COALESCE(SUM(e.co2eKg), 0)
			FROM EsgContribution e
			WHERE e.actorType = :actorType
			AND e.actorId = :actorId
			AND e.month >= :sinceMonth
			GROUP BY e.eventType
			""")
	List<Object[]> aggregateByEventTypeFromMonth(
			@Param("actorType") EsgActorType actorType,
			@Param("actorId") UUID actorId,
			@Param("sinceMonth") String sinceMonth);
}
