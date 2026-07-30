package com.cacib.messaging.application.service;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageStatus;
import com.cacib.messaging.domain.port.in.IngestMessageCommand;
import com.cacib.messaging.domain.port.in.IngestMessageUseCase;
import com.cacib.messaging.domain.port.in.IngestionOutcome;
import com.cacib.messaging.domain.port.out.MessageRepositoryPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.UUID;

public class MessageIngestionService implements IngestMessageUseCase {

    private static final Logger log = LoggerFactory.getLogger(MessageIngestionService.class);

    private final MessageRepositoryPort messageRepositoryPort;
    private final Clock clock;
    private final Counter persistedCounter;
    private final Counter duplicateCounter;
    private final Counter invalidCounter;

    public MessageIngestionService(MessageRepositoryPort messageRepositoryPort, Clock clock, MeterRegistry meterRegistry) {
        this.messageRepositoryPort = messageRepositoryPort;
        this.clock = clock;
        this.persistedCounter = meterRegistry.counter("messages.ingested", "outcome", "persisted");
        this.duplicateCounter = meterRegistry.counter("messages.ingested", "outcome", "duplicate");
        this.invalidCounter = meterRegistry.counter("messages.ingested", "outcome", "invalid");
    }

    @Override
    public IngestionOutcome ingest(IngestMessageCommand command) {
        var processedAt = clock.instant();
        // A blank payload can't be routed or displayed meaningfully, but it isn't an MQ/DB
        // failure either: retrying it via MQ redelivery would never succeed, so instead of
        // letting it loop into the backout queue (invisible to the DB/API/UI), it's captured
        // as ERROR — this is what makes MessageStatus.ERROR actually reachable.
        MessageStatus status = command.payload() == null || command.payload().isBlank()
                ? MessageStatus.ERROR
                : MessageStatus.PROCESSED;

        var message = new Message(
                UUID.randomUUID(),
                command.mqMessageId(),
                command.correlationId(),
                command.sourceQueue(),
                command.sourceApplication(),
                status,
                command.headers(),
                command.payload(),
                command.receivedAt(),
                processedAt
        );

        boolean inserted = messageRepositoryPort.insertIfAbsent(message);
        if (!inserted) {
            duplicateCounter.increment();
            log.info("Duplicate message received, mqMessageId={} already stored — skipping", command.mqMessageId());
            return IngestionOutcome.DUPLICATE;
        }

        if (status == MessageStatus.ERROR) {
            invalidCounter.increment();
            log.warn("Invalid message persisted as ERROR, mqMessageId={}, sourceQueue={} — blank payload",
                    command.mqMessageId(), command.sourceQueue());
            return IngestionOutcome.INVALID;
        }

        persistedCounter.increment();
        log.debug("Message persisted, mqMessageId={}, sourceQueue={}", command.mqMessageId(), command.sourceQueue());
        return IngestionOutcome.PERSISTED;
    }
}
