package com.cacib.messaging.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Message(
        UUID id,
        String mqMessageId,
        String correlationId,
        String sourceQueue,
        String sourceApplication,
        MessageStatus status,
        Map<String, String> headers,
        String payload,
        Instant receivedAt,
        Instant processedAt
) {
}
