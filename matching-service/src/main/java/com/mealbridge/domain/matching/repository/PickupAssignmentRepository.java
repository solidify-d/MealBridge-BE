package com.mealbridge.domain.matching.repository;

import com.mealbridge.domain.matching.entity.PickupAssignment;
import com.mealbridge.domain.matching.entity.PickupAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickupAssignmentRepository extends JpaRepository<PickupAssignment, UUID> {

	List<PickupAssignment> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
			UUID organizationId, PickupAssignmentStatus status);

	Optional<PickupAssignment> findByDonationIdAndOrganizationId(UUID donationId, UUID organizationId);

	List<PickupAssignment> findByDonationIdOrderByScoreDesc(UUID donationId);

	List<PickupAssignment> findByRoutePlanIdAndOrganizationId(UUID routePlanId, UUID organizationId);

	List<PickupAssignment> findByOrganizationIdAndRoutePlanIdIsNotNullAndStatusInOrderByCreatedAtDesc(
			UUID organizationId, List<PickupAssignmentStatus> statuses);

	List<PickupAssignment> findByOrganizationIdAndStatusInOrderByUpdatedAtDesc(
			UUID organizationId, List<PickupAssignmentStatus> statuses);
}
