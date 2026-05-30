package com.mealbridge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"mealbridge.gateway.internal-auth.enabled=true",
		"mealbridge.gateway.internal-secret=test-gateway-secret"
})
class GatewayInternalAuthWebFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void protectedPath_withoutGatewaySecret_returns403() throws Exception {
		mockMvc.perform(get("/admin/registrations/pending")
						.header("X-User-Id", "00000000-0000-0000-0000-000000000001")
						.header("X-User-Role", "SUPER_ADMIN"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status_message").value("Direct service access is not allowed"));
	}

	@Test
	void health_withoutGatewaySecret_returns200() throws Exception {
		mockMvc.perform(get("/health"))
				.andExpect(status().isOk());
	}
}
