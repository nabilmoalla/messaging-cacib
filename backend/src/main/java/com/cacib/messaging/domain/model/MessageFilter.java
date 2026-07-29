package com.cacib.messaging.domain.model;

import java.time.Instant;

public record MessageFilter(
        MessageStatus status,
        String sourceQueue,
        Instant from,
        Instant to
) {
    public static MessageFilter none() {
        return new MessageFilter(null, null, null, null);
    }
}
