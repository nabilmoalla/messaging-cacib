package com.cacib.messaging.application.service;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageStatus;
import com.cacib.messaging.domain.port.in.IngestMessageCommand;
import com.cacib.messaging.domain.port.in.IngestMessageUseCase;
import com.cacib.messaging.domain.port.in.IngestionOutcome;
import com.cacib.messaging.domain.port.out.MessageRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.UUID;

public class MessageIngestionService implements IngestMessageUseCase {

    private static final Logger log = LoggerFactory.getLogger(MessageIngestionService.class);

    private final MessageRepositoryPort messageRepositoryPort;
    private final Clock clock;

    public MessageIngestionService(MessageRepositoryPort messageRepositoryPort, Clock clock) {
        this.messageRepositoryPort = messageRepositoryPort;
        this.clock = clock;
    }

    @Override
    public IngestionOutcome ingest(IngestMessageCommand command) {
        var processedAt = clock.instant();
        var message = new Message(
                UUID.randomUUID(),
                command.mqMessageId(),
                command.correlationId(),
                command.sourceQueue(),
                command.sourceApplication(),
                MessageStatus.PROCESSED,
                command.headers(),
                command.payload(),
                command.receivedAt(),
                processedAt
        );

        boolean inserted = messageRepositoryPort.insertIfAbsent(message);
        if (!inserted) {
            log.info("Duplicate message received, mqMessageId={} already stored — skipping", command.mqMessageId());
            return IngestionOutcome.DUPLICATE;
        }

        log.debug("Message persisted, mqMessageId={}, sourceQueue={}", command.mqMessageId(), command.sourceQueue());
        return IngestionOutcome.PERSISTED;
    }
}
