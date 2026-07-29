package com.cacib.messaging.infrastructure.mq;

import com.cacib.messaging.domain.port.in.IngestMessageCommand;
import com.cacib.messaging.domain.port.in.IngestMessageUseCase;
import jakarta.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class MessageMqListener {

    private static final Logger log = LoggerFactory.getLogger(MessageMqListener.class);

    private final JmsMessageMapper mapper;
    private final IngestMessageUseCase ingestMessageUseCase;

    @Value("${app.mq.queue}")
    private String queueName;

    public MessageMqListener(JmsMessageMapper mapper, IngestMessageUseCase ingestMessageUseCase) {
        this.mapper = mapper;
        this.ingestMessageUseCase = ingestMessageUseCase;
    }

    @JmsListener(destination = "${app.mq.queue}", concurrency = "${app.mq.concurrency}")
    public void onMessage(jakarta.jms.Message message) throws JMSException {
        IngestMessageCommand command = mapper.toCommand(message, queueName);
        var outcome = ingestMessageUseCase.ingest(command);
        log.debug("mqMessageId={} sourceQueue={} outcome={}",
                command.mqMessageId(), command.sourceQueue(), outcome);
    }
}
