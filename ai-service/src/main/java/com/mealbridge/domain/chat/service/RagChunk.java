package com.mealbridge.domain.chat.service;

public record RagChunk(
		String document,
		String section,
		String content
) {
}
