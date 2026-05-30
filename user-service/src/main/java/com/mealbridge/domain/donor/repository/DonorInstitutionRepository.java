package com.mealbridge.domain.donor.repository;

import com.mealbridge.domain.donor.entity.DonorInstitution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DonorInstitutionRepository extends JpaRepository<DonorInstitution, UUID> {
}
