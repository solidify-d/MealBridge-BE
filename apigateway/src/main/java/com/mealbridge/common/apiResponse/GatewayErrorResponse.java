package com.mealbridge.common.apiResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GatewayErrorResponse {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private GatewayErrorResponse() {
	}

	public static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status_message", message);
		body.put("status_code", status.value());
		body.put("result", null);

		byte[] bytes;
		try {
			bytes = MAPPER.writeValueAsBytes(body);
		} catch (JsonProcessingException e) {
			bytes = ("{\"status_code\":" + status.value()
					+ ",\"status_message\":\"Internal error\",\"result\":null}").getBytes();
		}

		exchange.getResponse().setStatusCode(status);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
		DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
		return exchange.getResponse().writeWith(Mono.just(buffer));
	}
}
