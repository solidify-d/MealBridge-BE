package com.mealbridge.domain.chat.service;

import com.mealbridge.domain.chat.dto.ChatSourceDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RagRetriever {

	private final RagDocumentLoader documentLoader;

	public RagRetriever(RagDocumentLoader documentLoader) {
		this.documentLoader = documentLoader;
	}

	public List<RagChunk> retrieveTopChunks(String query, int limit, String portal) {
		if (!StringUtils.hasText(query)) {
			return List.of();
		}
		Set<String> terms = tokenize(query);
		List<ScoredChunk> scored = new ArrayList<>();
		for (RagChunk chunk : documentLoader.allChunks()) {
			if (!matchesPortal(chunk.document(), portal)) {
				continue;
			}
			int score = scoreChunk(chunk, terms);
			if (score > 0) {
				scored.add(new ScoredChunk(chunk, score));
			}
		}
		scored.sort(Comparator.comparingInt(ScoredChunk::score).reversed());
		return scored.stream()
				.limit(limit)
				.map(ScoredChunk::chunk)
				.toList();
	}

	public List<ChatSourceDto> toSources(List<RagChunk> chunks) {
		return chunks.stream()
				.map(c -> new ChatSourceDto(
						c.document(),
						c.section(),
						excerpt(c.content(), 200)))
				.toList();
	}

	private static int scoreChunk(RagChunk chunk, Set<String> terms) {
		String haystack = (chunk.document() + " " + chunk.section() + " " + chunk.content())
				.toLowerCase(Locale.ROOT);
		int score = 0;
		for (String term : terms) {
			if (haystack.contains(term)) {
				score += term.length() >= 4 ? 3 : 1;
			}
		}
		return score;
	}

	private static Set<String> tokenize(String query) {
		return List.of(query.toLowerCase(Locale.ROOT).split("\\s+")).stream()
				.map(t -> t.replaceAll("[^\\p{L}\\p{N}가-힣]", ""))
				.filter(t -> t.length() >= 2)
				.collect(Collectors.toSet());
	}

	private static String excerpt(String content, int maxLen) {
		String trimmed = content.replaceAll("\\s+", " ").trim();
		if (trimmed.length() <= maxLen) {
			return trimmed;
		}
		return trimmed.substring(0, maxLen) + "...";
	}

	private static boolean matchesPortal(String document, String portal) {
		boolean orgDoc = document != null && document.startsWith("organization-");
		if ("organization".equalsIgnoreCase(portal)) {
			return orgDoc;
		}
		return !orgDoc;
	}

	private record ScoredChunk(RagChunk chunk, int score) {
	}
}
