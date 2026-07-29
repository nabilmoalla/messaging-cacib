package com.cacib.messaging.infrastructure.web;

import java.util.UUID;

public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(UUID id) {
        super("No message found with id " + id);
    }
}
