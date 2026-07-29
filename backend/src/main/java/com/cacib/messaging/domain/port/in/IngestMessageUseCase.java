package com.cacib.messaging.domain.port.in;

public interface IngestMessageUseCase {

    IngestionOutcome ingest(IngestMessageCommand command);
}
