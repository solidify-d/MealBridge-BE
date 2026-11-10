package com.mealbridge.domain.vision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionJsonRepairTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void parsesBedrockResponseWithExtraClosingBracket() throws Exception {
		String malformed = """
				{"totalDeltaKg": 1.4, "items": [{"name": "Rice", "kg": 0.4, "category": "MAIN", "confidence": 0.95}, \
				{"name": "Fried fish", "kg": 0.1, "category": "SIDE", "confidence": 0.8}]]}""";

		BedrockVisionService service = new BedrockVisionService(true, "apac.amazon.nova-lite-v1:0", null);
		Method readRoot = BedrockVisionService.class.getDeclaredMethod("readVisionJsonRoot", String.class);
		readRoot.setAccessible(true);
		JsonNode root = (JsonNode) readRoot.invoke(service, malformed);

		assertEquals(1.4, root.get("totalDeltaKg").asDouble(), 0.001);
		assertTrue(root.get("items").isArray());
		assertEquals(2, root.get("items").size());
	}
}
