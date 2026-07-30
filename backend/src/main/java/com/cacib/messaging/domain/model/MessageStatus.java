package com.cacib.messaging.domain.model;

public enum MessageStatus {
    /**
     * Reserved for a future asynchronous processing step. Today ingestion validates and
     * persists a message in a single synchronous step, so every row lands directly as
     * {@link #PROCESSED} or {@link #ERROR} — this status is never set.
     */
    RECEIVED,
    PROCESSED,
    ERROR
}
