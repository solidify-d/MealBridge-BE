package com.mealbridge;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * Sample endpoint: trusts X-User-Id / X-User-Role injected by API Gateway after JWT validation.
 */
@RestController
@RequestMapping("/donations")
public class DonationMeController {

	@GetMapping("/me")
	public Map<String, Object> myDonations(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {

		if (userId == null || role == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing auth headers");
		}
		if (!"ORGANIZATION".equalsIgnoreCase(role) && !"DONOR".equalsIgnoreCase(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role not allowed for this resource");
		}

		return Map.of(
				"userId", UUID.fromString(userId),
				"role", role.toUpperCase(),
				"message", "Donation service received gateway auth headers"
		);
	}
}
