package com.cacib.messaging.application.service;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageFilter;
import com.cacib.messaging.domain.model.MessageOffsetPage;
import com.cacib.messaging.domain.model.MessageStats;
import com.cacib.messaging.domain.port.in.QueryMessagesUseCase;
import com.cacib.messaging.domain.port.out.MessageRepositoryPort;

import java.util.Optional;
import java.util.UUID;

public class MessageQueryService implements QueryMessagesUseCase {

    private final MessageRepositoryPort messageRepositoryPort;

    public MessageQueryService(MessageRepositoryPort messageRepositoryPort) {
        this.messageRepositoryPort = messageRepositoryPort;
    }

    @Override
    public MessageOffsetPage listByOffset(MessageFilter filter, int page, int size) {
        return messageRepositoryPort.findByOffset(filter, page, size);
    }

    @Override
    public Optional<Message> getById(UUID id) {
        return messageRepositoryPort.findById(id);
    }

    @Override
    public MessageStats getStats() {
        return messageRepositoryPort.getStats();
    }
}
