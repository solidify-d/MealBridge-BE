package com.mealbridge;

import com.mealbridge.domain.user.Role;
import com.mealbridge.domain.user.User;
import com.mealbridge.domain.user.UserRepository;
import com.mealbridge.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class SecurityErrorHandlingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String superAdminId;

	@BeforeEach
	void seedSuperAdmin() {
		User superAdmin = userRepository.findByEmail("super@mealbridge.local")
				.orElseGet(() -> userRepository.save(User.builder()
						.email("super@mealbridge.local")
						.password(passwordEncoder.encode("mealbridge123!"))
						.name("Super Admin")
						.phone("01000000001")
						.role(Role.SUPER_ADMIN)
						.status(UserStatus.ACTIVE)
						.build()));
		superAdminId = superAdmin.getId().toString();
	}

	@Test
	void unknownAdminPath_withAuth_returns404_not401() throws Exception {
		mockMvc.perform(get("/admin/registrations")
						.header("X-User-Id", superAdminId)
						.header("X-User-Role", "SUPER_ADMIN"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.statusCode").value(404));
	}

	@Test
	void unknownPath_withoutAuth_returns401() throws Exception {
		mockMvc.perform(get("/admin/registrations"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.statusCode").value(401));
	}

	@Test
	void invalidUuidPath_withAuth_returns400() throws Exception {
		mockMvc.perform(get("/admin/registrations/not-a-uuid")
						.header("X-User-Id", superAdminId)
						.header("X-User-Role", "SUPER_ADMIN"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.statusCode").value(400));
	}
}
