package com.mealbridge.domain.matching.service;

import com.mealbridge.domain.matching.dto.NotificationResDto;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class NotificationSseHub {

	private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

	private final Map<UUID, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

	public SseEmitter subscribe(UUID userId) {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
		emitter.onCompletion(() -> remove(userId, emitter));
		emitter.onTimeout(() -> remove(userId, emitter));
		emitter.onError(ex -> remove(userId, emitter));
		return emitter;
	}

	public void deliverLocally(UUID userId, NotificationResDto notification) {
		List<SseEmitter> emitters = emittersByUser.get(userId);
		if (emitters == null || emitters.isEmpty()) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event().name("notification").data(notification));
			} catch (IOException ex) {
				remove(userId, emitter);
			}
		}
	}

	private void remove(UUID userId, SseEmitter emitter) {
		List<SseEmitter> emitters = emittersByUser.get(userId);
		if (emitters != null) {
			emitters.remove(emitter);
		}
	}
}
