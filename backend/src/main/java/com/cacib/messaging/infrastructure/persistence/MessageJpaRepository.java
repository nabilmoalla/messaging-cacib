package com.cacib.messaging.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID>,
        JpaSpecificationExecutor<MessageEntity> {

    interface StatusCount {
        MessageStatusEntity getStatus();

        Long getMessageCount();
    }

    interface SourceQueueCount {
        String getSourceQueue();

        Long getMessageCount();
    }

    @Query("SELECT m.status AS status, COUNT(m) AS messageCount FROM MessageEntity m GROUP BY m.status")
    List<StatusCount> countGroupedByStatus();

    // source_queue is free-text with uncontrolled cardinality (new Back Office producers,
    // environment-specific names...), so this is capped to the top N by volume via the
    // Pageable rather than returning every distinct value ever seen.
    @Query("SELECT m.sourceQueue AS sourceQueue, COUNT(m) AS messageCount FROM MessageEntity m "
            + "GROUP BY m.sourceQueue ORDER BY COUNT(m) DESC")
    List<SourceQueueCount> countGroupedBySourceQueue(Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO message (id, mq_message_id, correlation_id, source_queue, source_application,
                                  status, headers, payload, received_at, processed_at)
            VALUES (:id, :mqMessageId, :correlationId, :sourceQueue, :sourceApplication,
                    :status, CAST(:headersJson AS jsonb), :payload, :receivedAt, :processedAt)
            ON CONFLICT (mq_message_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                        @Param("mqMessageId") String mqMessageId,
                        @Param("correlationId") String correlationId,
                        @Param("sourceQueue") String sourceQueue,
                        @Param("sourceApplication") String sourceApplication,
                        @Param("status") String status,
                        @Param("headersJson") String headersJson,
                        @Param("payload") String payload,
                        @Param("receivedAt") Instant receivedAt,
                        @Param("processedAt") Instant processedAt);
}
