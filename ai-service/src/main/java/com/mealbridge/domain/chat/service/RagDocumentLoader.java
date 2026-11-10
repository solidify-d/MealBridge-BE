package com.mealbridge.domain.chat.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class RagDocumentLoader {

	private static final Pattern SECTION_HEADER = Pattern.compile("(?m)^##\\s+(.+)$");

	private final String docsPath;
	private List<RagChunk> chunks = List.of();

	public RagDocumentLoader(@Value("${mealbridge.ai.rag.docs-path:../docs/rag}") String docsPath) {
		this.docsPath = docsPath;
	}

	@PostConstruct
	void load() {
		chunks = loadAllChunks();
		log.info("RAG loaded {} chunks from {}", chunks.size(), docsPath);
	}

	public List<RagChunk> allChunks() {
		return chunks;
	}

	private List<RagChunk> loadAllChunks() {
		Map<String, String> documents = new LinkedHashMap<>();
		loadFromFilesystem(documents);
		loadFromClasspath(documents);
		List<RagChunk> result = new ArrayList<>();
		for (Map.Entry<String, String> entry : documents.entrySet()) {
			result.addAll(splitBySections(entry.getKey(), entry.getValue()));
		}
		return List.copyOf(result);
	}

	private void loadFromFilesystem(Map<String, String> documents) {
		if (!StringUtils.hasText(docsPath) || docsPath.startsWith("classpath:") || docsPath.startsWith("file:")) {
			if (docsPath.startsWith("file:")) {
				Path dir = Path.of(docsPath.substring("file:".length())).toAbsolutePath().normalize();
				loadMarkdownFiles(documents, dir);
			}
			return;
		}
		loadMarkdownFiles(documents, Path.of(docsPath).toAbsolutePath().normalize());
	}

	private void loadMarkdownFiles(Map<String, String> documents, Path dir) {
		if (!Files.isDirectory(dir)) {
			log.debug("RAG docs directory not found: {}", dir);
			return;
		}
		try (var stream = Files.list(dir)) {
			stream.filter(p -> p.toString().endsWith(".md"))
					.forEach(p -> readFile(documents, p.getFileName().toString(), p));
		} catch (IOException e) {
			log.warn("Failed to list RAG docs at {}", dir, e);
		}
	}

	private void loadFromClasspath(Map<String, String> documents) {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver()
					.getResources("classpath:rag/*.md");
			for (Resource resource : resources) {
				String name = resource.getFilename();
				if (name != null && !documents.containsKey(name)) {
					documents.put(name, new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
				}
			}
		} catch (IOException e) {
			log.debug("No classpath RAG resources: {}", e.getMessage());
		}
	}

	private void readFile(Map<String, String> documents, String name, Path path) {
		try {
			documents.put(name, Files.readString(path, StandardCharsets.UTF_8));
		} catch (IOException e) {
			log.warn("Failed to read RAG file {}", path, e);
		}
	}

	static List<RagChunk> splitBySections(String document, String markdown) {
		List<RagChunk> chunks = new ArrayList<>();
		Matcher matcher = SECTION_HEADER.matcher(markdown);
		List<int[]> headers = new ArrayList<>();
		List<String> titles = new ArrayList<>();
		while (matcher.find()) {
			headers.add(new int[] {matcher.start(), matcher.end()});
			titles.add(matcher.group(1).trim());
		}
		if (headers.isEmpty()) {
			chunks.add(new RagChunk(document, "Overview", markdown.trim()));
			return chunks;
		}
		for (int i = 0; i < headers.size(); i++) {
			int contentStart = headers.get(i)[1];
			int contentEnd = i + 1 < headers.size() ? headers.get(i + 1)[0] : markdown.length();
			String body = markdown.substring(contentStart, contentEnd).trim();
			if (StringUtils.hasText(body)) {
				chunks.add(new RagChunk(document, titles.get(i), body));
			}
		}
		return chunks;
	}
}
