package com.mealbridge.domain.text.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealbridge.domain.text.dto.MealEnrichItemReq;
import com.mealbridge.domain.text.dto.MealEnrichItemRes;
import com.mealbridge.domain.text.dto.MealEnrichReqDto;
import com.mealbridge.domain.text.dto.MealEnrichResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAiMealEnrichService {

	private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
	private static final String MODEL = "gpt-4o-mini";

	private final boolean mockEnabled;
	private final String apiKey;
	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public OpenAiMealEnrichService(
			@Value("${mealbridge.ai.openai.mock:true}") boolean mockEnabled,
			@Value("${mealbridge.ai.openai.api-key:}") String apiKey) {
		this.mockEnabled = mockEnabled || !StringUtils.hasText(apiKey);
		this.apiKey = apiKey;
		this.restClient = RestClient.builder().build();
		if (this.mockEnabled) {
			log.info("OpenAI meal enrichment running in mock mode");
		}
	}

	public MealEnrichResDto enrich(MealEnrichReqDto request) {
		if (mockEnabled) {
			return mockEnrich(request);
		}
		try {
			String prompt = buildUserPrompt(request);
			String content = callOpenAi(prompt);
			return parseOpenAiResponse(request, content, false);
		} catch (Exception e) {
			log.error("OpenAI meal enrichment failed, falling back to mock", e);
			return mockEnrich(request);
		}
	}

	private String callOpenAi(String userPrompt) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", MODEL);
		body.put("temperature", 0.2);
		body.put("response_format", Map.of("type", "json_object"));
		body.put("messages", List.of(
				Map.of("role", "system", "content", """
						You are a Korean military cafeteria nutrition analyst. \
						Estimate per-serving nutrition and allergens for each dish. \
						Respond ONLY with JSON: {"items":[{"itemName":string,"caloriesKcal":number,\
						"proteinG":number,"carbsG":number,"fatG":number,"allergens":[string],\
						"allergyNotes":string,"summaryForNotes":string}]}
						"""),
				Map.of("role", "user", "content", userPrompt)));

		String responseBody = restClient.post()
				.uri(OPENAI_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + apiKey)
				.body(body)
				.retrieve()
				.body(String.class);

		JsonNode root = objectMapper.readTree(responseBody);
		return root.path("choices").path(0).path("message").path("content").asText("{}");
	}

	private String buildUserPrompt(MealEnrichReqDto request) throws Exception {
		return "Enrich nutrition for these cafeteria dishes (per serving):\n"
				+ objectMapper.writeValueAsString(request.items());
	}

	private MealEnrichResDto parseOpenAiResponse(MealEnrichReqDto request, String content, boolean mock) {
		try {
			JsonNode root = objectMapper.readTree(extractJson(content));
			List<MealEnrichItemRes> results = new ArrayList<>();
			JsonNode items = root.path("items");
			if (items.isArray()) {
				for (JsonNode node : items) {
					results.add(toItemRes(node));
				}
			}
			if (results.isEmpty()) {
				return mockEnrich(request);
			}
			alignItemNames(request, results);
			return new MealEnrichResDto(results);
		} catch (Exception e) {
			if (!mock) {
				log.warn("Invalid OpenAI JSON, using mock: {}", e.getMessage());
			}
			return mockEnrich(request);
		}
	}

	private void alignItemNames(MealEnrichReqDto request, List<MealEnrichItemRes> results) {
		for (int i = 0; i < request.items().size() && i < results.size(); i++) {
			MealEnrichItemReq req = request.items().get(i);
			MealEnrichItemRes res = results.get(i);
			if (!req.itemName().equals(res.itemName())) {
				results.set(i, new MealEnrichItemRes(
						req.itemName(),
						res.caloriesKcal(),
						res.proteinG(),
						res.carbsG(),
						res.fatG(),
						res.allergens(),
						res.allergyNotes(),
						res.summaryForNotes(),
						res.nutritionJson(),
						res.allergensJson()));
			}
		}
	}

	private MealEnrichItemRes toItemRes(JsonNode node) throws Exception {
		String itemName = node.path("itemName").asText("unknown");
		BigDecimal calories = decimal(node, "caloriesKcal", "180");
		BigDecimal protein = decimal(node, "proteinG", "8");
		BigDecimal carbs = decimal(node, "carbsG", "20");
		BigDecimal fat = decimal(node, "fatG", "6");
		List<String> allergens = new ArrayList<>();
		if (node.has("allergens") && node.get("allergens").isArray()) {
			for (JsonNode a : node.get("allergens")) {
				allergens.add(a.asText());
			}
		}
		String allergyNotes = node.path("allergyNotes").asText("");
		String summary = node.path("summaryForNotes").asText("");
		Map<String, Object> nutrition = Map.of(
				"caloriesKcal", calories,
				"proteinG", protein,
				"carbsG", carbs,
				"fatG", fat);
		String nutritionJson = objectMapper.writeValueAsString(nutrition);
		String allergensJson = objectMapper.writeValueAsString(allergens);
		return new MealEnrichItemRes(
				itemName, calories, protein, carbs, fat, allergens, allergyNotes, summary, nutritionJson, allergensJson);
	}

	private static BigDecimal decimal(JsonNode node, String field, String fallback) {
		if (node.has(field) && !node.get(field).isNull()) {
			return BigDecimal.valueOf(node.get(field).asDouble()).setScale(1, RoundingMode.HALF_UP);
		}
		return new BigDecimal(fallback);
	}

	private String extractJson(String text) {
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return text.substring(start, end + 1);
		}
		return text;
	}

	private MealEnrichResDto mockEnrich(MealEnrichReqDto request) {
		List<MealEnrichItemRes> items = request.items().stream()
				.map(this::mockItem)
				.toList();
		return new MealEnrichResDto(items);
	}

	private MealEnrichItemRes mockItem(MealEnrichItemReq req) {
		String name = req.itemName().toLowerCase();
		MockNutrition mock = selectMock(name, req.ingredients());
		try {
			Map<String, Object> nutrition = Map.of(
					"caloriesKcal", mock.calories(),
					"proteinG", mock.protein(),
					"carbsG", mock.carbs(),
					"fatG", mock.fat(),
					"preparedKg", req.preparedKg());
			String nutritionJson = objectMapper.writeValueAsString(nutrition);
			String allergensJson = objectMapper.writeValueAsString(mock.allergens());
			return new MealEnrichItemRes(
					req.itemName(),
					mock.calories(),
					mock.protein(),
					mock.carbs(),
					mock.fat(),
					mock.allergens(),
					mock.allergyNotes(),
					mock.summary(req.itemName(), req.preparedKg()),
					nutritionJson,
					allergensJson);
		} catch (Exception e) {
			throw new IllegalStateException("Mock enrichment failed", e);
		}
	}

	private MockNutrition selectMock(String name, String ingredients) {
		if (name.contains("김치") || contains(ingredients, "cabbage", "배추")) {
			return new MockNutrition(
					new BigDecimal("25"), new BigDecimal("1.5"), new BigDecimal("4"), new BigDecimal("0.5"),
					List.of("고춧가루"), "매운맛 알레르기 주의", "저칼로리 반찬");
		}
		if (name.contains("된장") || name.contains("찌개") || name.contains("국")) {
			return new MockNutrition(
					new BigDecimal("95"), new BigDecimal("6"), new BigDecimal("8"), new BigDecimal("4"),
					List.of("대두", "밀"), "된장·밀 알레르기 가능", "국물류 메뉴");
		}
		if (name.contains("제육") || name.contains("불고기") || contains(ingredients, "pork", "돼지")) {
			return new MockNutrition(
					new BigDecimal("320"), new BigDecimal("22"), new BigDecimal("12"), new BigDecimal("20"),
					List.of("돼지고기", "대두", "밀"), "고기·간장 알레르기 주의", "고단백 주식");
		}
		if (name.contains("비빔밥") || name.contains("밥")) {
			return new MockNutrition(
					new BigDecimal("450"), new BigDecimal("12"), new BigDecimal("72"), new BigDecimal("10"),
					List.of("계란", "대두", "참깨"), "계란·참깨 알레르기 가능", "탄수화물 중심");
		}
		return new MockNutrition(
				new BigDecimal("210"), new BigDecimal("10"), new BigDecimal("24"), new BigDecimal("8"),
				List.of("대두"), "일반 급식 알레르기 안내", "표준 급식");
	}

	private static boolean contains(String text, String... tokens) {
		if (text == null) {
			return false;
		}
		String lower = text.toLowerCase();
		for (String token : tokens) {
			if (lower.contains(token.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private record MockNutrition(
			BigDecimal calories,
			BigDecimal protein,
			BigDecimal carbs,
			BigDecimal fat,
			List<String> allergens,
			String allergyNotes,
			String category) {

		String summary(String itemName, BigDecimal preparedKg) {
			return itemName + " (" + category + ", 준비량 " + preparedKg + "kg) — 모의 영양 추정";
		}
	}
}
