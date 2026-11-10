package com.mealbridge.domain.procurement.service;

import com.mealbridge.domain.procurement.dto.ProcurementAdviseReqDto;
import com.mealbridge.domain.procurement.dto.ProcurementAdviseResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProcurementAdvisorService {

	private static final Pattern BULLET = Pattern.compile("^[-•*]\\s+(.+)$");

	private final boolean mockEnabled;
	private final String modelId;
	private final BedrockRuntimeClient bedrockClient;

	public ProcurementAdvisorService(
			@Value("${mealbridge.ai.mock:true}") boolean mockEnabled,
			@Value("${mealbridge.ai.bedrock.model-id:apac.amazon.nova-lite-v1:0}") String modelId,
				@Autowired(required = false) BedrockRuntimeClient bedrockClient) {
		this.mockEnabled = mockEnabled;
		this.modelId = modelId;
		this.bedrockClient = bedrockClient;
	}

	public ProcurementAdviseResDto advise(ProcurementAdviseReqDto request) {
		if (useMock()) {
			return mockAdvise(request);
		}
		try {
			String raw = invokeBedrock(request);
			return parseAiResponse(raw, false);
		} catch (Exception e) {
			log.error("Procurement Bedrock advise failed, using mock", e);
			return mockAdvise(request);
		}
	}

	private boolean useMock() {
		return mockEnabled || bedrockClient == null;
	}

	private String invokeBedrock(ProcurementAdviseReqDto request) {
		String systemPrompt = """
				You are MealBridge procurement advisor for military cafeteria donors. \
				Given JSON with historical waste stats, weather, and per-meal quantitative order suggestions, \
				write a concise English briefing for the kitchen manager.
				Format exactly:
				SUMMARY: (2-4 sentences overall strategy)
				HIGHLIGHTS:
				- (3-5 bullet actionable order adjustments referencing dates/meals/kg)
				CAUTIONS:
				- (1-3 risk bullets: weather, low confidence, missing plans)
				Use only data from the JSON. Do not invent numbers. Write all content in English.
				Donor institution: %s
				Data JSON:
				%s
				""".formatted(request.donorInstitutionId(), request.suggestionsJson());

		ConverseRequest converseRequest = ConverseRequest.builder()
				.modelId(modelId)
				.system(SystemContentBlock.fromText(systemPrompt))
				.messages(Message.builder()
						.role(ConversationRole.USER)
						.content(ContentBlock.fromText(
								"Based on the data above, write this week's optimal procurement quantity AI recommendations."))
						.build())
				.build();

		ConverseResponse response = bedrockClient.converse(converseRequest);
		return response.output().message().content().stream()
				.filter(block -> block.text() != null)
				.map(ContentBlock::text)
				.findFirst()
				.orElse("");
	}

	private ProcurementAdviseResDto parseAiResponse(String raw, boolean mock) {
		String summary = extractSection(raw, "SUMMARY:", "HIGHLIGHTS:");
		List<String> highlights = extractBullets(raw, "HIGHLIGHTS:", "CAUTIONS:");
		List<String> cautions = extractBullets(raw, "CAUTIONS:", null);

		if (summary.isBlank()) {
			summary = raw.trim();
		}
		if (highlights.isEmpty() && !summary.isBlank()) {
			highlights = List.of(summary);
			summary = "AI procurement briefing summary";
		}
		return new ProcurementAdviseResDto(summary.trim(), highlights, cautions, mock);
	}

	private String extractSection(String text, String startLabel, String endLabel) {
		int start = indexOfLabel(text, startLabel);
		if (start < 0) {
			return "";
		}
		start += startLabel.length();
		int end = endLabel != null ? indexOfLabel(text, endLabel) : text.length();
		if (end < 0) {
			end = text.length();
		}
		return text.substring(start, end).trim();
	}

	private List<String> extractBullets(String text, String startLabel, String endLabel) {
		String section = extractSection(text, startLabel, endLabel);
		List<String> bullets = new ArrayList<>();
		for (String line : section.split("\\R")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			Matcher matcher = BULLET.matcher(trimmed);
			if (matcher.matches()) {
				bullets.add(matcher.group(1).trim());
			} else if (!trimmed.endsWith(":")) {
				bullets.add(trimmed);
			}
		}
		return bullets;
	}

	private int indexOfLabel(String text, String label) {
		int idx = text.indexOf(label);
		if (idx >= 0) {
			return idx;
		}
		return text.toUpperCase().indexOf(label.toUpperCase());
	}

	private ProcurementAdviseResDto mockAdvise(ProcurementAdviseReqDto request) {
		String summary =
				"[Mock AI] Recommended order quantities per meal were calculated from historical leftovers, "
						+ "serving data, weather, day-of-week patterns, headcount, and menu similarity. "
						+ "Recommendations reflect per-person intake from similar sessions plus leftover prediction "
						+ "(targeting ~10% waste reduction). Use the per-item kg values in the table below to adjust your order sheet.";

		List<String> highlights = List.of(
				"For meals with registered plans, review the increase/decrease (%) vs the current plan.",
				"Items with live or historical weather reflect expected serving changes from temperature and humidity.",
				"For confidence scores below 0.7, verify on-site before finalizing orders.");

		List<String> cautions = List.of(
				"Mock AI mode — connect Bedrock for more detailed tailored recommendations.",
				"Meals without a registered plan use historical pattern estimates only; recalculate after adding the menu.");

		return new ProcurementAdviseResDto(summary, highlights, cautions, true);
	}
}
