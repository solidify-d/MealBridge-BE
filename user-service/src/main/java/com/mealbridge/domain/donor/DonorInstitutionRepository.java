package com.mealbridge.domain.donor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DonorInstitutionRepository extends JpaRepository<DonorInstitution, UUID> {
}
