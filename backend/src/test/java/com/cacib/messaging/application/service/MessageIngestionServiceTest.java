package com.cacib.messaging.application.service;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageStatus;
import com.cacib.messaging.domain.port.in.IngestMessageCommand;
import com.cacib.messaging.domain.port.in.IngestionOutcome;
import com.cacib.messaging.domain.port.out.MessageRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageIngestionServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-27T10:00:00Z");

    private MessageRepositoryPort repositoryPort;
    private MessageIngestionService service;

    @BeforeEach
    void setUp() {
        repositoryPort = mock(MessageRepositoryPort.class);
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new MessageIngestionService(repositoryPort, fixedClock);
    }

    @Test
    void persistsNewMessageAsProcessed() {
        var receivedAt = Instant.parse("2026-07-27T09:59:00Z");
        var command = new IngestMessageCommand(
                "mq-id-1", "corr-1", "DEV.QUEUE.1", "BACKOFFICE",
                Map.of("k", "v"), "<payload/>", receivedAt);
        when(repositoryPort.insertIfAbsent(any(Message.class))).thenReturn(true);

        var outcome = service.ingest(command);

        assertThat(outcome).isEqualTo(IngestionOutcome.PERSISTED);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(repositoryPort).insertIfAbsent(captor.capture());
        Message saved = captor.getValue();

        assertThat(saved.mqMessageId()).isEqualTo("mq-id-1");
        assertThat(saved.correlationId()).isEqualTo("corr-1");
        assertThat(saved.sourceQueue()).isEqualTo("DEV.QUEUE.1");
        assertThat(saved.sourceApplication()).isEqualTo("BACKOFFICE");
        assertThat(saved.status()).isEqualTo(MessageStatus.PROCESSED);
        assertThat(saved.headers()).containsEntry("k", "v");
        assertThat(saved.payload()).isEqualTo("<payload/>");
        assertThat(saved.receivedAt()).isEqualTo(receivedAt);
        assertThat(saved.processedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.id()).isNotNull();
    }

    @Test
    void redeliveredDuplicateIsSkippedWithoutError() {
        var command = new IngestMessageCommand(
                "mq-id-dup", null, "DEV.QUEUE.1", null,
                Map.of(), "<payload/>", Instant.parse("2026-07-27T09:59:00Z"));
        when(repositoryPort.insertIfAbsent(any(Message.class))).thenReturn(false);

        var outcome = service.ingest(command);

        assertThat(outcome).isEqualTo(IngestionOutcome.DUPLICATE);
    }
}
