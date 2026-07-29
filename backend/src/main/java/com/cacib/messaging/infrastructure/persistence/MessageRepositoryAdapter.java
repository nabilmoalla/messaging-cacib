package com.cacib.messaging.infrastructure.persistence;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageFilter;
import com.cacib.messaging.domain.model.MessageOffsetPage;
import com.cacib.messaging.domain.model.MessageStats;
import com.cacib.messaging.domain.model.MessageStatus;
import com.cacib.messaging.domain.port.out.MessageRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class MessageRepositoryAdapter implements MessageRepositoryPort {

    private static final String UNKNOWN_SOURCE_QUEUE = "UNKNOWN";
    private static final Sort DEFAULT_ORDER = Sort.by(Sort.Order.asc("receivedAt"), Sort.Order.asc("id"));

    private final MessageJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public MessageRepositoryAdapter(MessageJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public boolean insertIfAbsent(Message message) {
        int rowsInserted = jpaRepository.insertIfAbsent(
                message.id(),
                message.mqMessageId(),
                message.correlationId(),
                message.sourceQueue(),
                message.sourceApplication(),
                message.status().name(),
                objectMapper.writeValueAsString(message.headers() == null ? Map.of() : message.headers()),
                message.payload(),
                message.receivedAt(),
                message.processedAt()
        );
        return rowsInserted > 0;
    }

    @Override
    public MessageOffsetPage findByOffset(MessageFilter filter, int page, int size) {
        Page<MessageEntity> result = jpaRepository.findAll(
                MessageSpecifications.filter(filter),
                PageRequest.of(page, size, DEFAULT_ORDER));

        List<Message> content = result.getContent().stream()
                .map(MessageEntityMapper::toDomain)
                .toList();
        return new MessageOffsetPage(content, result.getTotalElements(), page, size);
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return jpaRepository.findById(id).map(MessageEntityMapper::toDomain);
    }

    @Override
    public MessageStats getStats() {
        Map<MessageStatus, Long> byStatus = jpaRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(
                        row -> MessageStatus.valueOf(row.getStatus().name()),
                        MessageJpaRepository.StatusCount::getMessageCount));

        Map<String, Long> bySourceQueue = jpaRepository.countGroupedBySourceQueue().stream()
                .collect(Collectors.toMap(
                        row -> row.getSourceQueue() == null ? UNKNOWN_SOURCE_QUEUE : row.getSourceQueue(),
                        MessageJpaRepository.SourceQueueCount::getMessageCount,
                        Long::sum,
                        LinkedHashMap::new));

        return new MessageStats(byStatus, bySourceQueue);
    }
}
