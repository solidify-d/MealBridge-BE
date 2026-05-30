package com.mealbridge.domain.chat.service;

import com.mealbridge.domain.chat.dto.ChatReqDto;
import com.mealbridge.domain.chat.dto.ChatResDto;
import com.mealbridge.domain.chat.dto.ChatSourceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ChatService {

	private final boolean mockEnabled;
	private final String modelId;
	private final BedrockRuntimeClient bedrockClient;
	private final RagRetriever ragRetriever;
	private final Map<UUID, List<String>> conversationHistory = new ConcurrentHashMap<>();

	public ChatService(
			@Value("${mealbridge.ai.mock:true}") boolean mockEnabled,
			@Value("${mealbridge.ai.bedrock.model-id:apac.amazon.nova-lite-v1:0}") String modelId,
			@Autowired(required = false) BedrockRuntimeClient bedrockClient,
			RagRetriever ragRetriever) {
		this.mockEnabled = mockEnabled;
		this.modelId = modelId;
		this.bedrockClient = bedrockClient;
		this.ragRetriever = ragRetriever;
	}

	public ChatResDto chat(ChatReqDto request) {
		UUID conversationId = request.conversationId() != null
				? request.conversationId()
				: UUID.randomUUID();

		String portal = resolvePortal(request);
		List<RagChunk> chunks = ragRetriever.retrieveTopChunks(request.message(), 3, portal);
		List<ChatSourceDto> sources = ragRetriever.toSources(chunks);

		if (useMock()) {
			return mockReply(request, conversationId, sources, chunks, portal);
		}

		try {
			String reply = invokeBedrock(request, chunks, portal);
			appendHistory(conversationId, request.message(), reply);
			return new ChatResDto(reply, conversationId, sources);
		} catch (Exception e) {
			log.error("Bedrock chat failed, using mock", e);
			return mockReply(request, conversationId, sources, chunks, portal);
		}
	}

	private boolean useMock() {
		return mockEnabled || bedrockClient == null;
	}

	private String invokeBedrock(ChatReqDto request, List<RagChunk> chunks, String portal) {
		String systemPrompt = buildSystemPrompt(request, chunks, portal);
		ConverseRequest converseRequest = ConverseRequest.builder()
				.modelId(modelId)
				.system(SystemContentBlock.fromText(systemPrompt))
				.messages(Message.builder()
						.role(ConversationRole.USER)
						.content(ContentBlock.fromText(request.message()))
						.build())
				.build();

		ConverseResponse response = bedrockClient.converse(converseRequest);
		return response.output().message().content().stream()
				.filter(block -> block.text() != null)
				.map(ContentBlock::text)
				.findFirst()
				.orElse("죄송합니다. 답변을 생성하지 못했습니다.");
	}

	private String buildSystemPrompt(ChatReqDto request, List<RagChunk> chunks, String portal) {
		StringBuilder rag = new StringBuilder();
		for (RagChunk chunk : chunks) {
			rag.append("\n### ").append(chunk.document()).append(" — ").append(chunk.section()).append("\n");
			rag.append(chunk.content()).append("\n");
		}
		String ctx = StringUtils.hasText(request.contextJson()) ? request.contextJson() : "{}";
		if ("organization".equals(portal)) {
			String orgId = request.organizationId() != null
					? request.organizationId().toString()
					: "unknown";
			return """
					You are MealBridge assistant for Korean welfare organizations picking up surplus military meals. \
					Answer in Korean, concisely and practically. Use the reference excerpts when relevant. \
					Help with pickup routes, meal selection, confirmations, transit, and notifications. \
					If unsure, say so and suggest checking the Pickup routes page or support.
					Organization: %s
					Request context JSON: %s
					Reference knowledge:
					%s
					""".formatted(orgId, ctx, rag);
		}
		String institution = request.donorInstitutionId() != null
				? request.donorInstitutionId().toString()
				: "unknown";
		return """
				You are MealBridge assistant for Korean military cafeteria donors. \
				Answer in Korean, concisely and practically. Use the reference excerpts when relevant. \
				If unsure, say so and suggest checking unit SOP.
				Donor institution: %s
				Request context JSON: %s
				Reference knowledge:
				%s
				""".formatted(institution, ctx, rag);
	}

	private String resolvePortal(ChatReqDto request) {
		if (request.organizationId() != null) {
			return "organization";
		}
		if (StringUtils.hasText(request.contextJson())
				&& request.contextJson().contains("\"portal\":\"organization\"")) {
			return "organization";
		}
		return "donor";
	}

	private ChatResDto mockReply(
			ChatReqDto request,
			UUID conversationId,
			List<ChatSourceDto> sources,
			List<RagChunk> chunks,
			String portal) {
		String hint = chunks.isEmpty()
				? ("organization".equals(portal) ? "픽업·잉여식품 안내" : "급식 운영 일반 안내")
				: chunks.get(0).section();
		String entityLabel = "organization".equals(portal)
				? (request.organizationId() != null ? request.organizationId().toString() : "미지정 기관")
				: (request.donorInstitutionId() != null ? request.donorInstitutionId().toString() : "미지정");
		String reply = """
				[모의 응답] '%s'에 대해 안내드립니다. \
				참고 자료(%s)를 바탕으로 도움을 드립니다. \
				기관/단체 ID: %s
				""".formatted(request.message(), hint, entityLabel);
		appendHistory(conversationId, request.message(), reply);
		return new ChatResDto(reply.trim(), conversationId, sources);
	}

	private void appendHistory(UUID conversationId, String userMsg, String assistantMsg) {
		conversationHistory.compute(conversationId, (id, list) -> {
			List<String> history = list != null ? new ArrayList<>(list) : new ArrayList<>();
			history.add("user:" + userMsg);
			history.add("assistant:" + assistantMsg);
			if (history.size() > 20) {
				return history.subList(history.size() - 20, history.size());
			}
			return history;
		});
	}
}
