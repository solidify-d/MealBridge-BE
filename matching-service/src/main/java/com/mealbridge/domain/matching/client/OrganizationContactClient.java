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
public class OrganizationContactClient {

	private static final String INTERNAL_SECRET_HEADER = "X-Gateway-Internal-Secret";

	private final RestTemplate loadBalancedRestTemplate;
	private final String internalSecret;

	public OrganizationContactClient(
			@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
			@Value("${mealbridge.gateway.internal-secret}") String internalSecret) {
		this.loadBalancedRestTemplate = loadBalancedRestTemplate;
		this.internalSecret = internalSecret;
	}

	public OrganizationContact fetchContact(UUID organizationId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(INTERNAL_SECRET_HEADER, internalSecret);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		Envelope response = loadBalancedRestTemplate.exchange(
				"http://user-service/internal/organizations/{id}/contact",
				HttpMethod.GET,
				entity,
				Envelope.class,
				organizationId).getBody();

		if (response == null || response.result == null) {
			throw new IllegalStateException("Failed to load organization contact: " + organizationId);
		}
		ContactResponse contact = response.result;
		return new OrganizationContact(
				contact.organizationId,
				contact.organizationName,
				contact.organizationPhone,
				contact.contactPersonName,
				contact.contactPersonPhone);
	}

	public record OrganizationContact(
			UUID organizationId,
			String organizationName,
			String organizationPhone,
			String contactPersonName,
			String contactPersonPhone) {
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Envelope {
		private ContactResponse result;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class ContactResponse {
		private UUID organizationId;
		private String organizationName;
		private String organizationPhone;
		private String contactPersonName;
		private String contactPersonPhone;
	}
}
