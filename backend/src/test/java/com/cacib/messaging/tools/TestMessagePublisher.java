package com.cacib.messaging.tools;

import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.Connection;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import java.util.UUID;

/**
 * Manual test producer for local development. The previously documented {@code amqsput}
 * sample utility performs a raw MQI put with no JMS headers, so it never sets
 * {@code JMSCorrelationID} or the {@code sourceApplication} property that
 * {@link com.cacib.messaging.infrastructure.mq.JmsMessageMapper} reads — messages sent that
 * way always land with correlation_id/source_application NULL. This publishes over JMS
 * instead, the same way {@code MessageMqListenerIT} does against a real IBM MQ container.
 *
 * <p>Run against the local docker-compose stack:
 * <pre>
 * mvn -q compile test-compile exec:java \
 *   -Dexec.mainClass=com.cacib.messaging.tools.TestMessagePublisher \
 *   -Dexec.classpathScope=test
 * </pre>
 * Reads the same MQ_* / APP_MQ_QUEUE environment variables as application.yml, defaulting to
 * the same values, so it works out of the box against docker-compose without extra config.
 */
public final class TestMessagePublisher {

    private TestMessagePublisher() {
    }

    public static void main(String[] args) throws Exception {
        String connName = env("MQ_CONN_NAME", "localhost(1414)");
        String host = connName.substring(0, connName.indexOf('('));
        int port = Integer.parseInt(connName.substring(connName.indexOf('(') + 1, connName.indexOf(')')));
        String channel = env("MQ_CHANNEL", "DEV.APP.SVRCONN");
        String queueManager = env("MQ_QMGR_NAME", "QM1");
        String user = env("MQ_APP_USER", "app");
        String password = env("MQ_APP_PASSWORD", "passw0rd");
        String queueName = env("APP_MQ_QUEUE", "DEV.QUEUE.1");
        String payload = env("MQ_PAYLOAD", "<payment><amount>100</amount></payment>");
        String sourceApplication = env("MQ_SOURCE_APPLICATION", "BACKOFFICE-TEST");
        String correlationId = env("MQ_CORRELATION_ID", UUID.randomUUID().toString());

        MQConnectionFactory connectionFactory = new MQConnectionFactory();
        connectionFactory.setHostName(host);
        connectionFactory.setPort(port);
        connectionFactory.setChannel(channel);
        connectionFactory.setQueueManager(queueManager);
        connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);

        try (Connection connection = connectionFactory.createConnection(user, password)) {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("queue:///" + queueName);
            MessageProducer producer = session.createProducer(queue);

            TextMessage message = session.createTextMessage(payload);
            message.setJMSCorrelationID(correlationId);
            message.setStringProperty("sourceApplication", sourceApplication);

            producer.send(message);
            session.commit();

            System.out.println("Published to " + queueName
                    + " correlationId=" + correlationId
                    + " sourceApplication=" + sourceApplication);
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
