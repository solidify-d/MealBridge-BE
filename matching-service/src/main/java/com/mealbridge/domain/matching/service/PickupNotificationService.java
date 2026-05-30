package com.mealbridge.domain.matching.service;

import com.mealbridge.domain.matching.client.AdminUserClient;
import com.mealbridge.domain.matching.client.DonationClient;
import com.mealbridge.domain.matching.client.DonationClient.DonationSnapshot;
import com.mealbridge.domain.matching.client.OrganizationContactClient;
import com.mealbridge.domain.matching.client.OrganizationContactClient.OrganizationContact;
import com.mealbridge.domain.matching.entity.NotificationType;
import com.mealbridge.domain.matching.repository.OrganizationPickupWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PickupNotificationService {

	private static final int WARNING_THRESHOLD = 5;

	private final NotificationService notificationService;
	private final OrganizationContactClient organizationContactClient;
	private final DonationClient donationClient;
	private final AdminUserClient adminUserClient;
	private final OrganizationPickupWarningRepository organizationPickupWarningRepository;

	public void notifyPickupConfirmed(
			UUID organizationId,
			UUID routePlanId,
			List<UUID> donationIds,
			Map<UUID, LocalDateTime> estimatedArrivals) {
		OrganizationContact contact = organizationContactClient.fetchContact(organizationId);
		for (UUID donationId : donationIds) {
			DonationSnapshot donation = donationClient.fetchById(donationId);
			LocalDateTime eta = estimatedArrivals.get(donationId);
			Map<String, Object> payload = buildPickupPayload(contact, donation, routePlanId, eta);
			String title = contact.organizationName() + " will pick up your surplus";
			String body = buildPickupBody(contact, donation, eta);
			notificationService.create(
					donation.registeredByUserId(),
					NotificationType.PICKUP_CONFIRMED,
					title,
					body,
					payload);
		}
	}

	public void notifyPickupEnRoute(
			UUID organizationId,
			UUID routePlanId,
			List<UUID> donationIds,
			Map<UUID, LocalDateTime> estimatedArrivals) {
		OrganizationContact contact = organizationContactClient.fetchContact(organizationId);
		for (UUID donationId : donationIds) {
			DonationSnapshot donation = donationClient.fetchById(donationId);
			LocalDateTime eta = estimatedArrivals.get(donationId);
			Map<String, Object> payload = buildPickupPayload(contact, donation, routePlanId, eta);
			notificationService.create(
					donation.registeredByUserId(),
					NotificationType.PICKUP_EN_ROUTE,
					contact.organizationName() + " is on the way",
					buildPickupBody(contact, donation, eta),
					payload);
		}
	}

	public void notifyPickupCompleted(UUID organizationId, UUID donationId) {
		OrganizationContact contact = organizationContactClient.fetchContact(organizationId);
		DonationSnapshot donation = donationClient.fetchById(donationId);
		Map<String, Object> payload = buildPickupPayload(contact, donation, null, null);
		notificationService.create(
				donation.registeredByUserId(),
				NotificationType.PICKUP_COMPLETED,
				"Pickup completed",
				contact.organizationName() + " completed pickup for " + donation.menuName(),
				payload);
	}

	public void notifyPickupCancelled(UUID organizationId, UUID donationId, String reason) {
		OrganizationContact contact = organizationContactClient.fetchContact(organizationId);
		DonationSnapshot donation = donationClient.fetchById(donationId);
		Map<String, Object> payload = buildPickupPayload(contact, donation, null, null);
		payload.put("reason", reason);
		notificationService.create(
				donation.registeredByUserId(),
				NotificationType.PICKUP_CANCELLED,
				"Pickup cancelled",
				contact.organizationName() + " cancelled pickup for " + donation.menuName(),
				payload);
	}

	public void recordWarningAndNotifyAdminsIfNeeded(
			UUID organizationId,
			UUID assignmentId,
			UUID donationId,
			String reason) {
		organizationPickupWarningRepository.save(
				com.mealbridge.domain.matching.entity.OrganizationPickupWarning.builder()
						.organizationId(organizationId)
						.pickupAssignmentId(assignmentId)
						.donationId(donationId)
						.reason(reason)
						.build());

		long warningCount = organizationPickupWarningRepository.countByOrganizationId(organizationId);
		if (warningCount < WARNING_THRESHOLD) {
			return;
		}

		OrganizationContact contact = organizationContactClient.fetchContact(organizationId);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("organizationId", organizationId.toString());
		payload.put("organizationName", contact.organizationName());
		payload.put("warningCount", warningCount);
		payload.put("donationId", donationId.toString());
		payload.put("reason", reason);

		String title = "Organization pickup warning threshold reached";
		String body = contact.organizationName() + " has " + warningCount + " in-transit pickup cancellations";

		for (UUID adminUserId : adminUserClient.fetchActiveAdminUserIds()) {
			notificationService.create(
					adminUserId,
					NotificationType.ADMIN_ORG_WARNING,
					title,
					body,
					payload);
		}
	}

	private Map<String, Object> buildPickupPayload(
			OrganizationContact contact,
			DonationSnapshot donation,
			UUID routePlanId,
			LocalDateTime eta) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("organizationId", contact.organizationId().toString());
		payload.put("organizationName", contact.organizationName());
		payload.put("organizationPhone", contact.organizationPhone());
		payload.put("contactPersonName", contact.contactPersonName());
		payload.put("contactPersonPhone", contact.contactPersonPhone());
		payload.put("donationId", donation.id().toString());
		payload.put("menuName", donation.menuName());
		if (routePlanId != null) {
			payload.put("routePlanId", routePlanId.toString());
		}
		if (eta != null) {
			payload.put("estimatedArrivalAt", eta.toString());
		}
		return payload;
	}

	private String buildPickupBody(
			OrganizationContact contact,
			DonationSnapshot donation,
			LocalDateTime eta) {
		StringBuilder sb = new StringBuilder();
		sb.append(contact.organizationName());
		if (eta != null) {
			sb.append(" · ETA ").append(eta.toLocalTime());
		}
		sb.append(" · ").append(donation.menuName());
		if (contact.organizationPhone() != null && !contact.organizationPhone().isBlank()) {
			sb.append(" · Tel ").append(contact.organizationPhone());
		}
		return sb.toString();
	}
}
