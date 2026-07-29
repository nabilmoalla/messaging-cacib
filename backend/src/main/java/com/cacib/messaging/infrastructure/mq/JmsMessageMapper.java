package com.cacib.messaging.infrastructure.mq;

import com.cacib.messaging.domain.port.in.IngestMessageCommand;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
public class JmsMessageMapper {

    private final Clock clock;

    public JmsMessageMapper(Clock clock) {
        this.clock = clock;
    }

    /**
     * The source queue is passed in rather than read from {@code JMSDestination}: that header
     * is only populated when the original producer sent the message via JMS. Back Office
     * applications typically put messages using the native MQI, so it is frequently absent —
     * the queue the listener is bound to is the only reliable source.
     */
    public IngestMessageCommand toCommand(jakarta.jms.Message message, String sourceQueue) throws JMSException {
        if (!(message instanceof TextMessage textMessage)) {
            throw new IllegalArgumentException(
                    "Unsupported JMS message type: " + message.getClass().getName() + " — expected TextMessage");
        }

        return new IngestMessageCommand(
                message.getJMSMessageID(),
                message.getJMSCorrelationID(),
                sourceQueue,
                message.getStringProperty("sourceApplication"),
                headers(message),
                textMessage.getText(),
                clock.instant()
        );
    }

    private Map<String, String> headers(jakarta.jms.Message message) throws JMSException {
        Map<String, String> headers = new HashMap<>();
        Enumeration<?> propertyNames = message.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = (String) propertyNames.nextElement();
            Object value = message.getObjectProperty(name);
            headers.put(name, value == null ? null : value.toString());
        }
        return Collections.unmodifiableMap(headers);
    }
}
