package com.mealbridge.domain.mealplan.repository;

import com.mealbridge.domain.donation.entity.MealType;
import com.mealbridge.domain.mealplan.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealPlanRepository extends JpaRepository<MealPlan, UUID> {

	List<MealPlan> findByDonorInstitutionIdAndPlanDateBetweenOrderByPlanDateAscMealTypeAsc(
			UUID donorInstitutionId,
			LocalDate from,
			LocalDate to);

	Optional<MealPlan> findByDonorInstitutionIdAndPlanDateAndMealType(
			UUID donorInstitutionId,
			LocalDate planDate,
			MealType mealType);
}
