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

import java.util.UUID;

@Component
public class DonorSettingsClient {

	private static final String INTERNAL_SECRET_HEADER = "X-Gateway-Internal-Secret";

	private final RestTemplate loadBalancedRestTemplate;
	private final String internalSecret;

	public DonorSettingsClient(
			@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
			@Value("${mealbridge.gateway.internal-secret}") String internalSecret) {
		this.loadBalancedRestTemplate = loadBalancedRestTemplate;
		this.internalSecret = internalSecret;
	}

	public DonorSettings fetchSettings(UUID donorInstitutionId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(INTERNAL_SECRET_HEADER, internalSecret);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		Envelope response = loadBalancedRestTemplate.exchange(
				"http://user-service/internal/donor-institutions/{id}/settings",
				HttpMethod.GET,
				entity,
				Envelope.class,
				donorInstitutionId).getBody();

		if (response == null || response.result == null) {
			return new DonorSettings(donorInstitutionId, "PER_MENU");
		}
		return new DonorSettings(response.result.id, response.result.pickupAcceptanceMode);
	}

	public record DonorSettings(UUID id, String pickupAcceptanceMode) {
		public boolean isSessionBulk() {
			return "SESSION_BULK".equalsIgnoreCase(pickupAcceptanceMode);
		}
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Envelope {
		private SettingsResponse result;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class SettingsResponse {
		private UUID id;
		private String pickupAcceptanceMode;
	}
}
