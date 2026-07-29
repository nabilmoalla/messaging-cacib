package com.cacib.messaging.config;

import com.cacib.messaging.application.service.MessageIngestionService;
import com.cacib.messaging.application.service.MessageQueryService;
import com.cacib.messaging.domain.port.in.IngestMessageUseCase;
import com.cacib.messaging.domain.port.in.QueryMessagesUseCase;
import com.cacib.messaging.domain.port.out.MessageRepositoryPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public IngestMessageUseCase ingestMessageUseCase(MessageRepositoryPort messageRepositoryPort, Clock clock,
                                                      MeterRegistry meterRegistry) {
        return new MessageIngestionService(messageRepositoryPort, clock, meterRegistry);
    }

    @Bean
    public QueryMessagesUseCase queryMessagesUseCase(MessageRepositoryPort messageRepositoryPort) {
        return new MessageQueryService(messageRepositoryPort);
    }
}
