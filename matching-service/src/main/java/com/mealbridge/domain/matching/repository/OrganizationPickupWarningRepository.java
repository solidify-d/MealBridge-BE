package com.mealbridge.domain.matching.repository;

import com.mealbridge.domain.matching.entity.OrganizationPickupWarning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationPickupWarningRepository extends JpaRepository<OrganizationPickupWarning, UUID> {

	long countByOrganizationId(UUID organizationId);

	java.util.Optional<OrganizationPickupWarning> findByPickupAssignmentId(UUID pickupAssignmentId);
}
