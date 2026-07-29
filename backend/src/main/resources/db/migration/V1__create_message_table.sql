CREATE TABLE message (
    id                  UUID PRIMARY KEY,
    mq_message_id       VARCHAR(255) NOT NULL,
    correlation_id      VARCHAR(255),
    source_queue        VARCHAR(255),
    source_application  VARCHAR(255),
    status              VARCHAR(20) NOT NULL,
    headers             JSONB,
    payload             TEXT,
    received_at         TIMESTAMPTZ NOT NULL,
    processed_at        TIMESTAMPTZ,

    CONSTRAINT uq_message_mq_message_id UNIQUE (mq_message_id),
    CONSTRAINT chk_message_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'ERROR'))
);

CREATE INDEX idx_message_correlation_id ON message (correlation_id);
CREATE INDEX idx_message_source_queue ON message (source_queue);
CREATE INDEX idx_message_source_application ON message (source_application);
CREATE INDEX idx_message_status ON message (status);
CREATE INDEX idx_message_received_at ON message (received_at);
