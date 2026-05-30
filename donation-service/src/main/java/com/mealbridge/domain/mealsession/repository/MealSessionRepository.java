package com.mealbridge.domain.mealsession.repository;

import com.mealbridge.domain.mealsession.entity.MealSession;
import com.mealbridge.domain.mealsession.entity.MealSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealSessionRepository extends JpaRepository<MealSession, UUID> {

	Optional<MealSession> findFirstByDonorInstitutionIdAndRegisteredByUserIdAndStatusInOrderByCreatedAtDesc(
			UUID donorInstitutionId,
			UUID registeredByUserId,
			Collection<MealSessionStatus> statuses);

	List<MealSession> findByDonorInstitutionIdAndSessionDateBetweenOrderBySessionDateAscMealTypeAsc(
			UUID donorInstitutionId,
			LocalDate from,
			LocalDate to);
}
