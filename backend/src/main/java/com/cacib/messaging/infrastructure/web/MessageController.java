package com.cacib.messaging.infrastructure.web;

import com.cacib.messaging.domain.model.MessageFilter;
import com.cacib.messaging.domain.model.MessageOffsetPage;
import com.cacib.messaging.domain.model.MessageStats;
import com.cacib.messaging.domain.model.MessageStatus;
import com.cacib.messaging.domain.port.in.QueryMessagesUseCase;
import com.cacib.messaging.infrastructure.web.dto.MessagePageResponse;
import com.cacib.messaging.infrastructure.web.dto.MessageResponse;
import com.cacib.messaging.infrastructure.web.dto.MessageStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@Validated
public class MessageController {

    private final QueryMessagesUseCase queryMessagesUseCase;
    private final MessageWebMapper mapper;

    public MessageController(QueryMessagesUseCase queryMessagesUseCase, MessageWebMapper mapper) {
        this.queryMessagesUseCase = queryMessagesUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List messages", description = "Paginated (page/size) and filterable list of messages.")
    public MessagePageResponse list(
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) String sourceQueue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) int size
    ) {
        MessageFilter filter = new MessageFilter(status, sourceQueue, from, to);
        MessageOffsetPage result = queryMessagesUseCase.listByOffset(filter, page, size);
        return toResponse(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a message by id")
    public MessageResponse getById(@PathVariable UUID id) {
        return queryMessagesUseCase.getById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new MessageNotFoundException(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Message counts by status and by source queue")
    public MessageStatsResponse stats() {
        MessageStats stats = queryMessagesUseCase.getStats();
        return new MessageStatsResponse(toStringKeyedMap(stats), stats.bySourceQueue());
    }

    private MessagePageResponse toResponse(MessageOffsetPage page) {
        var content = page.content().stream().map(mapper::toResponse).toList();
        int totalPages = page.size() == 0 ? 0 : (int) Math.ceil((double) page.totalElements() / page.size());
        return new MessagePageResponse(content, page.page(), page.size(), page.totalElements(), totalPages);
    }

    private Map<String, Long> toStringKeyedMap(MessageStats stats) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        stats.byStatus().forEach((status, count) -> byStatus.put(status.name(), count));
        return byStatus;
    }
}
