package com.cacib.messaging.domain.port.out;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageFilter;
import com.cacib.messaging.domain.model.MessageOffsetPage;
import com.cacib.messaging.domain.model.MessageStats;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepositoryPort {

    /**
     * Persists the message unless a message with the same mqMessageId already exists.
     *
     * @return true if the message was newly inserted, false if it was a duplicate (no-op)
     */
    boolean insertIfAbsent(Message message);

    MessageOffsetPage findByOffset(MessageFilter filter, int page, int size);

    Optional<Message> findById(UUID id);

    MessageStats getStats();
}
