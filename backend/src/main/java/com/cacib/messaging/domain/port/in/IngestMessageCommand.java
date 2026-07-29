package com.cacib.messaging.domain.port.in;

import java.time.Instant;
import java.util.Map;

public record IngestMessageCommand(
        String mqMessageId,
        String correlationId,
        String sourceQueue,
        String sourceApplication,
        Map<String, String> headers,
        String payload,
        Instant receivedAt
) {
}
