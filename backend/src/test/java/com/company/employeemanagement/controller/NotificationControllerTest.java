package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.response.MessageResponse;
import com.company.employeemanagement.dto.response.NotificationResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UnreadCountResponse;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link NotificationController} using standalone MockMvc.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private NotificationResponse buildResponse(final UUID id, final boolean read) {
        return new NotificationResponse(
                id,
                NotificationType.TASK_ASSIGNED,
                "New Task Assigned",
                "You have been assigned a task.",
                UUID.randomUUID(),
                read,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("GET /notifications")
    class FindMyNotifications {

        @Test
        @DisplayName("returns 200 with notification list")
        void returns200() throws Exception {
            UUID id = UUID.randomUUID();
            NotificationResponse notif = buildResponse(id, false);
            PageResponse<NotificationResponse> page = PageResponse.from(
                    new PageImpl<>(List.of(notif), PageRequest.of(0, 20), 1L));

            when(notificationService.findMyNotifications(any())).thenReturn(page);

            mockMvc.perform(get("/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                    .andExpect(jsonPath("$.content[0].read").value(false));
        }
    }

    @Nested
    @DisplayName("GET /notifications/unread-count")
    class GetUnreadCount {

        @Test
        @DisplayName("returns 200 with unread count")
        void returns200() throws Exception {
            when(notificationService.getUnreadCount()).thenReturn(new UnreadCountResponse(7L));

            mockMvc.perform(get("/notifications/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(7));
        }
    }

    @Nested
    @DisplayName("PATCH /notifications/{id}/read")
    class MarkAsRead {

        @Test
        @DisplayName("returns 200 when successfully marked as read")
        void returns200() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(notificationService).markAsRead(id);

            mockMvc.perform(patch("/notifications/{id}/read", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Notification marked as read."));
        }

        @Test
        @DisplayName("returns 404 when notification not found")
        void returns404() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Notification", id))
                    .when(notificationService).markAsRead(id);

            mockMvc.perform(patch("/notifications/{id}/read", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when trying to mark another user's notification")
        void returns403WhenWrongRecipient() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new AccessDeniedException("You may only modify your own notifications."))
                    .when(notificationService).markAsRead(id);

            mockMvc.perform(patch("/notifications/{id}/read", id))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /notifications/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("returns 200 when all marked as read")
        void returns200() throws Exception {
            doNothing().when(notificationService).markAllAsRead();

            mockMvc.perform(patch("/notifications/read-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("All notifications marked as read."));
        }
    }
}
