package com.cacib.messaging.infrastructure.web;

import com.cacib.messaging.domain.model.Message;
import com.cacib.messaging.domain.model.MessageOffsetPage;
import com.cacib.messaging.domain.model.MessageStats;
import com.cacib.messaging.domain.model.MessageStatus;
import com.cacib.messaging.domain.port.in.QueryMessagesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@Import(MessageWebMapperImpl.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryMessagesUseCase queryMessagesUseCase;

    private Message sampleMessage(UUID id) {
        return new Message(
                id, "mq-1", "corr-1", "DEV.QUEUE.1", "BACKOFFICE",
                MessageStatus.PROCESSED, Map.of("k", "v"), "<payload/>",
                Instant.parse("2026-07-27T09:00:00Z"), Instant.parse("2026-07-27T09:00:01Z"));
    }

    @Test
    void listOffset_returnsPage() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryMessagesUseCase.listByOffset(any(), eq(0), eq(50)))
                .thenReturn(new MessageOffsetPage(List.of(sampleMessage(id)), 1, 0, 50));

        mockMvc.perform(get("/api/v1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void listWithSizeBelowMinimum_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/messages").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listWithInvalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/messages").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_found_returnsMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryMessagesUseCase.getById(id)).thenReturn(Optional.of(sampleMessage(id)));

        mockMvc.perform(get("/api/v1/messages/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryMessagesUseCase.getById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/messages/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/messages/" + id));
    }

    @Test
    void stats_returnsCounts() throws Exception {
        when(queryMessagesUseCase.getStats()).thenReturn(new MessageStats(
                Map.of(MessageStatus.PROCESSED, 5L),
                Map.of("DEV.QUEUE.1", 5L)));

        mockMvc.perform(get("/api/v1/messages/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.PROCESSED").value(5))
                .andExpect(jsonPath("$.bySourceQueue['DEV.QUEUE.1']").value(5));
    }
}
