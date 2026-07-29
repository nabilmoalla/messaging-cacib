package com.cacib.messaging.infrastructure.web.dto;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
