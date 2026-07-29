package com.cacib.messaging.domain.port.in;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageFilter;
import com.cacib.messaging.domain.model.MessageOffsetPage;
import com.cacib.messaging.domain.model.MessageStats;

import java.util.Optional;
import java.util.UUID;

public interface QueryMessagesUseCase {

    MessageOffsetPage listByOffset(MessageFilter filter, int page, int size);

    Optional<Message> getById(UUID id);

    MessageStats getStats();
}
