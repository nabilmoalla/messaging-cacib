package com.cacib.messaging.domain.model;

import java.util.Map;

public record MessageStats(Map<MessageStatus, Long> byStatus, Map<String, Long> bySourceQueue) {
}
