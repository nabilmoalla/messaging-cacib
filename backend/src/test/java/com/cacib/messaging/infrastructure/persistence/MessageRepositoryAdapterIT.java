package com.cacib.messaging.infrastructure.persistence;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MessageRepositoryAdapter.class, JacksonAutoConfiguration.class})
class MessageRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MessageRepositoryAdapter adapter;

    private Message newMessage(String mqMessageId) {
        return new Message(
                UUID.randomUUID(),
                mqMessageId,
                "corr-1",
                "DEV.QUEUE.1",
                "BACKOFFICE",
                MessageStatus.PROCESSED,
                Map.of("k", "v"),
                "<payload/>",
                Instant.parse("2026-07-27T09:00:00Z"),
                Instant.parse("2026-07-27T09:00:01Z")
        );
    }

    @Test
    void firstInsertSucceedsAndRedeliveredDuplicateIsIgnored() {
        String mqMessageId = "mq-" + UUID.randomUUID();

        boolean firstInsert = adapter.insertIfAbsent(newMessage(mqMessageId));
        boolean secondInsert = adapter.insertIfAbsent(newMessage(mqMessageId));

        assertThat(firstInsert).isTrue();
        assertThat(secondInsert).isFalse();
    }
}
