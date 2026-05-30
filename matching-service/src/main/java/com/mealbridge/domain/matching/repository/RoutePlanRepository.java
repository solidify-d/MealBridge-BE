package com.mealbridge.domain.matching.repository;

import com.mealbridge.domain.matching.entity.RoutePlan;
import com.mealbridge.domain.matching.entity.RoutePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, UUID> {

	List<RoutePlan> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
			UUID organizationId, RoutePlanStatus status);
}
