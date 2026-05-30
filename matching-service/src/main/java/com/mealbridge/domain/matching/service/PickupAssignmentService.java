package com.mealbridge.domain.matching.service;

import com.mealbridge.domain.matching.client.DonationClient;
import com.mealbridge.domain.matching.dto.AcceptAssignmentReq;
import com.mealbridge.domain.matching.dto.ConfirmPickupPlanReq;
import com.mealbridge.domain.matching.dto.PickupAssignmentResDto;
import com.mealbridge.domain.matching.entity.PickupAssignment;
import com.mealbridge.domain.matching.entity.PickupAssignmentStatus;
import com.mealbridge.domain.matching.repository.PickupAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PickupAssignmentService {

	private final PickupAssignmentRepository pickupAssignmentRepository;
	private final DonationClient donationClient;

	public PickupAssignmentResDto accept(UUID actorUserId, AcceptAssignmentReq request) {
		donationClient.fetchById(request.donationId());
		donationClient.acceptDonation(request.donationId(), request.organizationId(), actorUserId);

		PickupAssignment assignment = pickupAssignmentRepository
				.findByDonationIdAndOrganizationId(request.donationId(), request.organizationId())
				.orElseGet(() -> PickupAssignment.builder()
						.donationId(request.donationId())
						.organizationId(request.organizationId())
						.status(PickupAssignmentStatus.ACCEPTED)
						.score(0)
						.build());

		if (assignment.getStatus() != PickupAssignmentStatus.ACCEPTED) {
			assignment.accept();
		}
		pickupAssignmentRepository.save(assignment);
		return toDto(assignment);
	}

	@Transactional(readOnly = true)
	public List<PickupAssignmentResDto> listAccepted(UUID organizationId) {
		return pickupAssignmentRepository
				.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, PickupAssignmentStatus.ACCEPTED)
				.stream()
				.map(this::toDto)
				.toList();
	}

	@Transactional
	public void confirmPlan(UUID actorUserId, ConfirmPickupPlanReq request) {
		for (UUID donationId : request.donationIds()) {
			accept(actorUserId, new AcceptAssignmentReq(donationId, request.organizationId()));
		}
	}

	private PickupAssignmentResDto toDto(PickupAssignment assignment) {
		return new PickupAssignmentResDto(
				assignment.getId(),
				assignment.getDonationId(),
				assignment.getOrganizationId(),
				assignment.getStatus(),
				assignment.getScore(),
				assignment.getCreatedAt());
	}
}
