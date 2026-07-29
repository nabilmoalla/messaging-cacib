package com.cacib.messaging.infrastructure.mq;

import com.cacib.messaging.infrastructure.persistence.MessageEntity;
import com.cacib.messaging.infrastructure.persistence.MessageJpaRepository;
import com.cacib.messaging.infrastructure.persistence.MessageStatusEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: publishes a message on DEV.QUEUE.1 and verifies the listener consumes and
 * persists it. Starts a real IBM MQ container, so it is tagged "slow" and only runs with
 * {@code mvn verify} (failsafe), not the default {@code mvn test}.
 */
@Tag("slow")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MessageMqListenerIT {

    private static final String QUEUE_MANAGER = "QM1";
    private static final String CHANNEL = "DEV.APP.SVRCONN";
    private static final String APP_PASSWORD = "passw0rd";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> ibmMq = new GenericContainer<>(
            DockerImageName.parse("icr.io/ibm-messaging/mq:9.4.0.0-r3"))
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", QUEUE_MANAGER)
            .withEnv("MQ_APP_PASSWORD", APP_PASSWORD)
            .withExposedPorts(1414)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));

    @DynamicPropertySource
    static void mqProperties(DynamicPropertyRegistry registry) {
        registry.add("ibm.mq.conn-name", () -> ibmMq.getHost() + "(" + ibmMq.getMappedPort(1414) + ")");
        registry.add("ibm.mq.queue-manager", () -> QUEUE_MANAGER);
        registry.add("ibm.mq.channel", () -> CHANNEL);
        registry.add("ibm.mq.user", () -> "app");
        registry.add("ibm.mq.password", () -> APP_PASSWORD);
    }

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @Test
    void consumesMessageFromQueueAndPersistsIt() throws InterruptedException {
        String correlationId = UUID.randomUUID().toString();

        sendWithRetry(correlationId);

        MessageEntity persisted = awaitMessageByCorrelationId(correlationId);

        assertThat(persisted.getSourceQueue()).isEqualTo("DEV.QUEUE.1");
        assertThat(persisted.getSourceApplication()).isEqualTo("BACKOFFICE-TEST");
        assertThat(persisted.getPayload()).contains("100");
        assertThat(persisted.getStatus()).isEqualTo(MessageStatusEntity.PROCESSED);
        assertThat(persisted.getMqMessageId()).isNotBlank();
    }

    /**
     * The container's mapped port can accept TCP connections slightly before the queue
     * manager is ready to authenticate JMS connections, so the first send attempts are
     * retried rather than relying on a specific log message from the image.
     */
    private void sendWithRetry(String correlationId) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(90);
        JmsException lastFailure = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                jmsTemplate.send("DEV.QUEUE.1", session -> {
                    var textMessage = session.createTextMessage("<payment><amount>100</amount></payment>");
                    textMessage.setJMSCorrelationID(correlationId);
                    textMessage.setStringProperty("sourceApplication", "BACKOFFICE-TEST");
                    return textMessage;
                });
                return;
            } catch (JmsException e) {
                lastFailure = e;
                Thread.sleep(2000);
            }
        }
        throw new IllegalStateException("Unable to send test message to IBM MQ within timeout", lastFailure);
    }

    private MessageEntity awaitMessageByCorrelationId(String correlationId) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(30);
        while (Instant.now().isBefore(deadline)) {
            List<MessageEntity> all = messageJpaRepository.findAll();
            Optional<MessageEntity> match = all.stream()
                    .filter(entity -> correlationId.equals(entity.getCorrelationId()))
                    .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Message with correlationId=" + correlationId + " was not persisted in time");
    }
}
