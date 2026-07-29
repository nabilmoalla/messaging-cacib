package com.cacib.messaging.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "message")
public class MessageEntity {

    @Id
    private UUID id;

    @Column(name = "mq_message_id", nullable = false, unique = true)
    private String mqMessageId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "source_queue")
    private String sourceQueue;

    @Column(name = "source_application")
    private String sourceApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatusEntity status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private Map<String, String> headers;

    @Column(name = "payload")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected MessageEntity() {
        // JPA
    }

    public MessageEntity(UUID id, String mqMessageId, String correlationId, String sourceQueue,
                          String sourceApplication, MessageStatusEntity status, Map<String, String> headers,
                          String payload, Instant receivedAt, Instant processedAt) {
        this.id = id;
        this.mqMessageId = mqMessageId;
        this.correlationId = correlationId;
        this.sourceQueue = sourceQueue;
        this.sourceApplication = sourceApplication;
        this.status = status;
        this.headers = headers;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getMqMessageId() {
        return mqMessageId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getSourceQueue() {
        return sourceQueue;
    }

    public String getSourceApplication() {
        return sourceApplication;
    }

    public MessageStatusEntity getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
