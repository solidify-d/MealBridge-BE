package com.mealbridge.domain.weather.client;

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
public class DonorInstitutionClient {

	private static final String INTERNAL_SECRET_HEADER = "X-Gateway-Internal-Secret";

	private final RestTemplate loadBalancedRestTemplate;
	private final String internalSecret;

	public DonorInstitutionClient(
			@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
			@Value("${mealbridge.gateway.internal-secret}") String internalSecret) {
		this.loadBalancedRestTemplate = loadBalancedRestTemplate;
		this.internalSecret = internalSecret;
	}

	public InstitutionLocation fetchLocation(UUID donorInstitutionId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(INTERNAL_SECRET_HEADER, internalSecret);
		HttpEntity<Void> entity = new HttpEntity<>(headers);

		Envelope response = loadBalancedRestTemplate.exchange(
				"http://user-service/internal/donor-institutions/{id}/location",
				HttpMethod.GET,
				entity,
				Envelope.class,
				donorInstitutionId).getBody();

		if (response == null || response.result == null) {
			throw new IllegalStateException("Failed to load donor institution location");
		}

		InstitutionLocationResponse location = response.result;
		return new InstitutionLocation(
				location.id,
				location.name,
				location.address,
				location.latitude,
				location.longitude);
	}

	public record InstitutionLocation(
			UUID id,
			String name,
			String address,
			Double latitude,
			Double longitude) {
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Envelope {
		private InstitutionLocationResponse result;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class InstitutionLocationResponse {
		private UUID id;
		private String name;
		private String address;
		private Double latitude;
		private Double longitude;
	}
}
