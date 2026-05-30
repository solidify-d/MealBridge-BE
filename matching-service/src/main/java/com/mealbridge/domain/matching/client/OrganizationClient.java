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

import java.util.List;
import java.util.UUID;

@Component
public class OrganizationClient {

	private static final String INTERNAL_SECRET_HEADER = "X-Gateway-Internal-Secret";

	private final RestTemplate loadBalancedRestTemplate;
	private final String internalSecret;

	public OrganizationClient(
			@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
			@Value("${mealbridge.gateway.internal-secret}") String internalSecret) {
		this.loadBalancedRestTemplate = loadBalancedRestTemplate;
		this.internalSecret = internalSecret;
	}

	public List<OrganizationSnapshot> fetchActiveOrganizations() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(INTERNAL_SECRET_HEADER, internalSecret);
		HttpEntity<Void> entity = new HttpEntity<>(headers);

		ListEnvelope response = loadBalancedRestTemplate.exchange(
				"http://user-service/internal/organizations/active",
				HttpMethod.GET,
				entity,
				ListEnvelope.class).getBody();

		if (response == null || response.result == null) {
			throw new IllegalStateException("Failed to load active organizations");
		}

		return response.result.stream()
				.map(org -> new OrganizationSnapshot(
						org.id,
						org.name,
						org.address,
						org.latitude,
						org.longitude,
						org.recipientCapacity,
						org.serviceArea,
						org.targetDemographic,
						org.maxPickupDistanceKm,
						org.desiredPickupWeightKg,
						org.servingStartTime))
				.toList();
	}

	public OrganizationSnapshot fetchById(UUID organizationId) {
		return fetchActiveOrganizations().stream()
				.filter(org -> org.id().equals(organizationId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Active organization not found: " + organizationId));
	}

	public record OrganizationSnapshot(
			UUID id,
			String name,
			String address,
			Double latitude,
			Double longitude,
			Integer recipientCapacity,
			String serviceArea,
			String targetDemographic,
			Double maxPickupDistanceKm,
			Double desiredPickupWeightKg,
			String servingStartTime) {
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class ListEnvelope {
		private List<OrganizationResponse> result;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class OrganizationResponse {
		private UUID id;
		private String name;
		private String address;
		private Double latitude;
		private Double longitude;
		private Integer recipientCapacity;
		private String serviceArea;
		private String targetDemographic;
		private Double maxPickupDistanceKm;
		private Double desiredPickupWeightKg;
		private String servingStartTime;
	}
}
