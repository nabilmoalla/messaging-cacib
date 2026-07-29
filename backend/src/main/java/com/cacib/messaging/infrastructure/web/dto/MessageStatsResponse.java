package com.cacib.messaging.infrastructure.web.dto;

import java.util.Map;

public record MessageStatsResponse(
        Map<String, Long> byStatus,
        Map<String, Long> bySourceQueue
) {
}
