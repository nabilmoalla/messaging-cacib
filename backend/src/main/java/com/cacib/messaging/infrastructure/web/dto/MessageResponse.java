package com.cacib.messaging.infrastructure.web.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        String mqMessageId,
        String correlationId,
        String sourceQueue,
        String sourceApplication,
        String status,
        Map<String, String> headers,
        String payload,
        Instant receivedAt,
        Instant processedAt
) {
}
