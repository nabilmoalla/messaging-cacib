package com.cacib.messaging.infrastructure.web;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.infrastructure.web.dto.MessageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MessageWebMapper {

    MessageResponse toResponse(Message message);
}
