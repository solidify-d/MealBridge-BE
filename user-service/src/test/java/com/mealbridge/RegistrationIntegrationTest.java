package com.mealbridge;

import com.mealbridge.domain.organization.entity.OrganizationType;
import com.mealbridge.domain.organization.entity.TargetDemographic;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class RegistrationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registerOrganization_pendingUntilApproved() throws Exception {
		String email = "neworg@mealbridge.local";

		mockMvc.perform(post("/auth/register/organization")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "mealbridge123!",
								  "name": "Org Contact",
								  "phone": "01011112222",
								  "organizationName": "Hope Center",
								  "organizationType": "WELFARE_CENTER",
								  "address": "Seoul Gangnam",
								  "organizationPhone": "0211112222",
								  "recipientCapacity": 30,
								  "targetDemographic": "ELDERLY"
								}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result.status").value("PENDING"));

		User user = userRepository.findByEmail(email).orElseThrow();
		assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
		assertThat(user.getRole()).isEqualTo(Role.ORGANIZATION);
		assertThat(organizationRepository.findById(user.getOrganizationId())).isPresent();

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"mealbridge123!"}
								""".formatted(email)))
				.andExpect(status().isUnauthorized());

		seedSuperAdmin();
		mockMvc.perform(patch("/admin/registrations/{id}/approve", user.getId())
						.header("X-User-Id", findSuperAdminId())
						.header("X-User-Role", "SUPER_ADMIN"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"mealbridge123!"}
								""".formatted(email)))
				.andExpect(status().isOk());
	}

	@Test
	void registerDonor_duplicateEmail_returns400() throws Exception {
		String email = "dupdonor@mealbridge.local";
		String body = """
				{
				  "email": "%s",
				  "password": "mealbridge123!",
				  "name": "Donor Contact",
				  "phone": "01033334444",
				  "institutionName": "Demo School",
				  "institutionType": "SCHOOL",
				  "address": "Busan",
				  "institutionPhone": "0511112222"
				}
				""".formatted(email);

		mockMvc.perform(post("/auth/register/donor")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/auth/register/donor")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	private void seedSuperAdmin() {
		if (userRepository.findByEmail("super@mealbridge.local").isPresent()) {
			return;
		}
		userRepository.save(User.builder()
				.email("super@mealbridge.local")
				.password(passwordEncoder.encode("mealbridge123!"))
				.name("Super Admin")
				.phone("01000000001")
				.role(Role.SUPER_ADMIN)
				.status(UserStatus.ACTIVE)
				.build());
	}

	private String findSuperAdminId() {
		return userRepository.findByEmail("super@mealbridge.local")
				.orElseThrow()
				.getId()
				.toString();
	}
}
