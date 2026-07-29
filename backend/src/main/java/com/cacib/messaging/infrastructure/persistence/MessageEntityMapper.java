package com.cacib.messaging.infrastructure.persistence;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageStatus;

final class MessageEntityMapper {

    private MessageEntityMapper() {
    }

    static Message toDomain(MessageEntity entity) {
        return new Message(
                entity.getId(),
                entity.getMqMessageId(),
                entity.getCorrelationId(),
                entity.getSourceQueue(),
                entity.getSourceApplication(),
                MessageStatus.valueOf(entity.getStatus().name()),
                entity.getHeaders(),
                entity.getPayload(),
                entity.getReceivedAt(),
                entity.getProcessedAt()
        );
    }
}
