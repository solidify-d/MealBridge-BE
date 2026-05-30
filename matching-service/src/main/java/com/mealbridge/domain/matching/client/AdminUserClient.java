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
public class AdminUserClient {

	private static final String INTERNAL_SECRET_HEADER = "X-Gateway-Internal-Secret";

	private final RestTemplate loadBalancedRestTemplate;
	private final String internalSecret;

	public AdminUserClient(
			@Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
			@Value("${mealbridge.gateway.internal-secret}") String internalSecret) {
		this.loadBalancedRestTemplate = loadBalancedRestTemplate;
		this.internalSecret = internalSecret;
	}

	public List<UUID> fetchActiveAdminUserIds() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(INTERNAL_SECRET_HEADER, internalSecret);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		ListEnvelope response = loadBalancedRestTemplate.exchange(
				"http://user-service/internal/admins/active-user-ids",
				HttpMethod.GET,
				entity,
				ListEnvelope.class).getBody();

		if (response == null || response.result == null) {
			return List.of();
		}
		return response.result;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class ListEnvelope {
		private List<UUID> result;
	}
}
