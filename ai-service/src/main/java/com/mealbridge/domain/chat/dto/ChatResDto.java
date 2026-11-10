package com.mealbridge.domain.chat.dto;

import java.util.List;
import java.util.UUID;

public record ChatResDto(
		String reply,
		UUID conversationId,
		List<ChatSourceDto> sources
) {
}
