package com.mealbridge.domain.matching.dto;

import java.util.UUID;

public record NotificationSseEvent(UUID recipientUserId, NotificationResDto notification) {
}
