package com.mealbridge.domain.matching.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class DonationClient {

	private static final String INTERNAL_SECRET_HEADER = "X-Gateway-Internal-Secret";

	private final RestTemplate loadBalancedRestTemplate;
	private final String internalSecret;

	public DonationClient(
			@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
			@Value("${mealbridge.gateway.internal-secret}") String internalSecret) {
		this.loadBalancedRestTemplate = loadBalancedRestTemplate;
		this.internalSecret = internalSecret;
	}

	public DonationSnapshot fetchById(UUID donationId) {
		HttpEntity<Void> entity = new HttpEntity<>(internalHeaders());
		Envelope response = loadBalancedRestTemplate.exchange(
				"http://donation-service/internal/donations/{id}",
				HttpMethod.GET,
				entity,
				Envelope.class,
				donationId).getBody();

		if (response == null || response.result == null) {
			throw new IllegalStateException("Failed to load donation: " + donationId);
		}
		return toSnapshot(response.result);
	}

	public List<DonationSnapshot> fetchRegisteredAvailable() {
		HttpEntity<Void> entity = new HttpEntity<>(internalHeaders());
		ListEnvelope response = loadBalancedRestTemplate.exchange(
				"http://donation-service/internal/donations/registered",
				HttpMethod.GET,
				entity,
				ListEnvelope.class).getBody();

		if (response == null || response.result == null) {
			throw new IllegalStateException("Failed to load registered donations");
		}
		return response.result.stream().map(this::toSnapshot).toList();
	}

	public void acceptDonation(UUID donationId, UUID organizationId, UUID actorUserId) {
		postOrganizationAction(donationId, organizationId, actorUserId, "/accept");
	}

	public void markInTransit(UUID donationId, UUID organizationId, UUID actorUserId) {
		postOrganizationAction(donationId, organizationId, actorUserId, "/in-transit");
	}

	public void completeDonation(UUID donationId, UUID organizationId, UUID actorUserId) {
		postOrganizationAction(donationId, organizationId, actorUserId, "/complete");
	}

	public void releaseDonation(UUID donationId, UUID organizationId, UUID actorUserId) {
		postOrganizationAction(donationId, organizationId, actorUserId, "/release");
	}

	private void postOrganizationAction(
			UUID donationId, UUID organizationId, UUID actorUserId, String suffix) {
		HttpHeaders headers = internalHeaders();
		headers.set("X-User-Id", actorUserId.toString());
		headers.set("X-User-Role", "ORGANIZATION");
		HttpEntity<AcceptBody> entity = new HttpEntity<>(new AcceptBody(organizationId), headers);
		loadBalancedRestTemplate.exchange(
				"http://donation-service/donations/{id}" + suffix,
				HttpMethod.POST,
				entity,
				Void.class,
				donationId);
	}

	private DonationSnapshot toSnapshot(DonationResponse dto) {
		return new DonationSnapshot(
				dto.id,
				dto.donorInstitutionId,
				dto.registeredByUserId,
				dto.mealSessionId,
				dto.status,
				dto.mealType,
				dto.menuName,
				dto.pickupDeadlineAt,
				dto.notes,
				dto.totalWeightKg,
				dto.beforeImageUrl,
				dto.afterImageUrl,
				dto.lineItemsJson,
				dto.acceptedOrganizationId,
				dto.acceptedAt,
				dto.inTransitAt,
				dto.completedAt,
				dto.createdAt,
				dto.updatedAt);
	}

	private HttpHeaders internalHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(INTERNAL_SECRET_HEADER, internalSecret);
		return headers;
	}

	public record DonationSnapshot(
			UUID id,
			UUID donorInstitutionId,
			UUID registeredByUserId,
			UUID mealSessionId,
			String status,
			String mealType,
			String menuName,
			LocalDateTime pickupDeadlineAt,
			String notes,
			BigDecimal totalWeightKg,
			String beforeImageUrl,
			String afterImageUrl,
			String lineItemsJson,
			UUID acceptedOrganizationId,
			LocalDateTime acceptedAt,
			LocalDateTime inTransitAt,
			LocalDateTime completedAt,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
	}

	private record AcceptBody(UUID organizationId) {
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Envelope {
		private DonationResponse result;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class ListEnvelope {
		private List<DonationResponse> result;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class DonationResponse {
		private UUID id;
		private UUID donorInstitutionId;
		private UUID registeredByUserId;
		private UUID mealSessionId;
		private String status;
		private String mealType;
		private String menuName;
		private LocalDateTime pickupDeadlineAt;
		private String notes;
		private BigDecimal totalWeightKg;
		private String beforeImageUrl;
		private String afterImageUrl;
		private String lineItemsJson;
		private UUID acceptedOrganizationId;
		private LocalDateTime acceptedAt;
		private LocalDateTime inTransitAt;
		private LocalDateTime completedAt;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
	}
}
