package com.mealbridge.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JsonUtil() {
	}

	public static String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Failed to serialize JSON", e);
		}
	}

	public static <T> T fromJson(String json, TypeReference<T> type) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return MAPPER.readValue(json, type);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Failed to parse JSON", e);
		}
	}
}
