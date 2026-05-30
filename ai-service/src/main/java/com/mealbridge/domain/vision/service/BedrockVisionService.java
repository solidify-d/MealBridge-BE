package com.mealbridge.domain.vision.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealbridge.domain.vision.VisionMode;
import com.mealbridge.domain.vision.dto.VisionAnalyzeResDto;
import com.mealbridge.domain.vision.dto.VisionItemEstimate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ImageBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ImageFormat;
import software.amazon.awssdk.services.bedrockruntime.model.ImageSource;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class BedrockVisionService {

	private final boolean mockEnabled;
	private final String modelId;
	private final BedrockRuntimeClient bedrockClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public BedrockVisionService(
			@Value("${mealbridge.ai.mock:false}") boolean mockEnabled,
			@Value("${mealbridge.ai.bedrock.model-id:apac.amazon.nova-lite-v1:0}") String modelId,
			@Autowired(required = false) BedrockRuntimeClient bedrockClient) {
		this.mockEnabled = mockEnabled;
		this.modelId = modelId;
		this.bedrockClient = bedrockClient;
	}

	public VisionAnalyzeResDto analyze(VisionMode mode, MultipartFile image, String sessionContext) {
		if (mockEnabled) {
			return mockResponse(mode);
		}
		if (bedrockClient == null) {
			throw new IllegalStateException(
					"Bedrock client is not configured. Set mealbridge.ai.mock=false and cloud.aws.credentials.");
		}
		try {
			byte[] bytes = image.getBytes();
			String prompt = buildPrompt(mode, sessionContext);
			String text = invokeBedrock(bytes, image.getContentType(), prompt);
			return parseResponse(mode, text, false);
		} catch (Exception e) {
			log.error("Bedrock vision failed", e);
			throw new IllegalStateException("Vision analysis failed: " + e.getMessage(), e);
		}
	}

	private String invokeBedrock(byte[] bytes, String contentType, String prompt) {
		ImageFormat format = contentType != null && contentType.contains("png") ? ImageFormat.PNG : ImageFormat.JPEG;
		ConverseRequest request = ConverseRequest.builder()
				.modelId(modelId)
				.messages(Message.builder()
						.role(ConversationRole.USER)
						.content(
								ContentBlock.fromImage(ImageBlock.builder()
										.format(format)
										.source(ImageSource.builder()
												.bytes(SdkBytes.fromByteArray(bytes))
												.build())
										.build()),
								ContentBlock.fromText(prompt))
						.build())
				.build();

		ConverseResponse response = bedrockClient.converse(request);
		return response.output().message().content().stream()
				.filter(block -> block.text() != null)
				.map(ContentBlock::text)
				.findFirst()
				.orElse("{}");
	}

	private String buildPrompt(VisionMode mode, String sessionContext) {
		String ctx = StringUtils.hasText(sessionContext) ? sessionContext : "{}";
		if (mode == VisionMode.SERVING) {
			return """
					You analyze a cafeteria serving line camera frame. Estimate how much food (in kg) was \
					taken/served since the last observation. Context JSON: %s
					Respond ONLY with JSON: {"totalDeltaKg": number, "items": [{"name": string, "kg": number, "confidence": 0-1}]}
					""".formatted(ctx);
		}
		if (mode == VisionMode.MEAL_REGISTER) {
			return """
					You analyze a photo of a military/unit cafeteria prepared meal before serving. \
					Identify each visible dish: main (주식), side dishes (반찬), soup (국). \
					Estimate per-person serving weight in kg for each dish (1인분), not total batch weight. \
					Context JSON may include headcount; still return per-person kg only. Context JSON: %s
					Respond ONLY with JSON: {"totalDeltaKg": number, "items": [{"name": string, "kg": number, \
					"category": "MAIN"|"SIDE"|"SOUP", "confidence": 0-1}]}
					""".formatted(ctx);
		}
		return """
				You analyze leftover food on trays after a meal. Estimate per-dish leftover weight in kg. \
				Context JSON: %s
				Respond ONLY with JSON: {"totalDeltaKg": number, "items": [{"name": string, "kg": number, "confidence": 0-1}]}
				""".formatted(ctx);
	}

	private VisionAnalyzeResDto parseResponse(VisionMode mode, String text, boolean mock) {
		try {
			JsonNode root = readVisionJsonRoot(text);
			BigDecimal total = root.has("totalDeltaKg")
					? BigDecimal.valueOf(root.get("totalDeltaKg").asDouble())
					: BigDecimal.ZERO;
			List<VisionItemEstimate> items = new ArrayList<>();
			if (root.has("items") && root.get("items").isArray()) {
				for (JsonNode node : root.get("items")) {
					items.add(new VisionItemEstimate(
							node.path("name").asText("unknown"),
							BigDecimal.valueOf(node.path("kg").asDouble(0)),
							node.path("confidence").asDouble(0.5)));
				}
			}
			if (items.isEmpty()) {
				throw new IllegalArgumentException("No items in vision response");
			}
			if (total.signum() == 0 && !items.isEmpty()) {
				total = items.stream()
						.map(VisionItemEstimate::kg)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
			}
			return new VisionAnalyzeResDto(mode, total, items, text, mock);
		} catch (Exception e) {
			if (!mock) {
				throw new IllegalStateException("Invalid vision JSON from model: " + text, e);
			}
			log.warn("Failed to parse vision JSON: {}", e.getMessage());
			return mockResponse(mode);
		}
	}

	private JsonNode readVisionJsonRoot(String text) {
		String extracted = extractJson(text);
		JsonProcessingException lastError = null;
		for (String candidate : repairJsonCandidates(extracted)) {
			try {
				JsonNode root = objectMapper.readTree(candidate);
				log.debug("Parsed vision JSON after repair (len={})", candidate.length());
				return root;
			} catch (JsonProcessingException ex) {
				lastError = ex;
			}
		}
		if (lastError != null) {
			throw new IllegalArgumentException("Could not parse vision JSON", lastError);
		}
		throw new IllegalArgumentException("Empty vision JSON");
	}

	private List<String> repairJsonCandidates(String json) {
		List<String> candidates = new ArrayList<>();
		String sanitized = sanitizeVisionJson(json);
		String balanced = balanceJsonBrackets(sanitized);
		candidates.add(sanitized);
		if (!balanced.equals(sanitized)) {
			candidates.add(balanced);
		}
		if (!json.equals(sanitized)) {
			candidates.add(json);
		}
		return candidates;
	}

	/** Fixes common Bedrock typos such as trailing `]]}` instead of `]}`. */
	private String sanitizeVisionJson(String json) {
		String s = json.trim();
		if (s.endsWith("]]}")) {
			s = s.substring(0, s.length() - 3) + "]}";
		}
		s = s.replaceAll("\\]\\s*\\]\\s*}", "]}");
		s = s.replaceAll("\\]\\s*\\]\\s*,", "],");
		while (s.endsWith("]]")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	/** Removes extra `]` when the model closes arrays/objects one bracket too many. */
	private String balanceJsonBrackets(String json) {
		String s = json;
		int openSquare = 0;
		int closeSquare = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '[') {
				openSquare++;
			} else if (c == ']') {
				closeSquare++;
			}
		}
		while (closeSquare > openSquare && s.endsWith("]")) {
			s = s.substring(0, s.length() - 1);
			closeSquare--;
		}
		if (!s.endsWith("}") && s.contains("{")) {
			s = s + "}";
		}
		return s;
	}

	private String extractJson(String text) {
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return text.substring(start, end + 1);
		}
		return text;
	}

	private VisionAnalyzeResDto mockResponse(VisionMode mode) {
		if (mode == VisionMode.SERVING) {
			return new VisionAnalyzeResDto(
					mode,
					new BigDecimal("1.200"),
					List.of(new VisionItemEstimate("main dish", new BigDecimal("1.200"), 0.75)),
					"{\"mock\":true}",
					true);
		}
		if (mode == VisionMode.MEAL_REGISTER) {
			return new VisionAnalyzeResDto(
					mode,
					new BigDecimal("0.450"),
					List.of(
							new VisionItemEstimate("제육볶음", new BigDecimal("0.250"), 0.82),
							new VisionItemEstimate("배추김치", new BigDecimal("0.050"), 0.74),
							new VisionItemEstimate("된장찌개", new BigDecimal("0.150"), 0.7)),
					"{\"mock\":true}",
					true);
		}
		return new VisionAnalyzeResDto(
				mode,
				new BigDecimal("4.500"),
				List.of(
						new VisionItemEstimate("main dish", new BigDecimal("2.500"), 0.7),
						new VisionItemEstimate("side dish", new BigDecimal("2.000"), 0.65)),
				"{\"mock\":true}",
				true);
	}
}
