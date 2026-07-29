package com.cacib.messaging.domain.model;

import java.util.List;

public record MessageOffsetPage(List<Message> content, long totalElements, int page, int size) {
}
