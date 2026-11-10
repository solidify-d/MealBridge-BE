package com.mealbridge.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtil {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

	public static <T> T fromJson(String json, Class<T> type) {
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
