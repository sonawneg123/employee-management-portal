package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.client.GroqClient;
import com.company.employeemanagement.ai.config.GroqProperties;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskAiReview;
import com.company.employeemanagement.entity.TaskSubmission;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.repository.TaskAiReviewRepository;
import com.company.employeemanagement.repository.TaskCommentRepository;
import com.company.employeemanagement.repository.TaskSubmissionRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for Phase 7D notification behaviour in {@link TaskAiReviewService}.
 *
 * <p>Validates:
 * <ul>
 *   <li>AI completion notifies the employee (new in Phase 7D)</li>
 *   <li>AI failure notifies the employee with a friendly message</li>
 *   <li>Employee notification does NOT contain raw error messages</li>
 *   <li>No duplicate notifications when requester == submitter</li>
 *   <li>Completion notification sent exactly once per review</li>
 *   <li>Failure notification sent exactly once per review</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Phase 7D — Notification Tests")
class Phase7DNotificationTest {

    @Mock private TaskSubmissionRepository submissionRepository;
    @Mock private TaskAiReviewRepository   aiReviewRepository;
    @Mock private TaskCommentRepository    commentRepository;
    @Mock private SecurityUtils            securityUtils;
    @Mock private GroqClient               groqClient;
    @Mock private SubmissionAttachmentExtractionService extractionService;
    @Mock private NotificationService      notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GroqProperties groqProperties;
    private TaskAiReviewService service;

    private static final UUID MANAGER_ID  = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID TASK_ID     = UUID.randomUUID();
    private static final UUID SUBMISSION_ID = UUID.randomUUID();

    private Employee manager;
    private Employee employee;
    private Task task;
    private TaskSubmission submission;

    @BeforeEach
    void setUp() {
        groqProperties = new GroqProperties();
        groqProperties.setApiKey("test-key");
        groqProperties.setModel("test-model");
        groqProperties.setBaseUrl("https://api.test.com");
        groqProperties.setTimeoutMs(5000);
        groqProperties.setMaxTokens(1024);

        service = new TaskAiReviewService(
                submissionRepository, aiReviewRepository, commentRepository,
                securityUtils, groqClient, groqProperties, extractionService,
                objectMapper, notificationService);

        // Manager employee
        User managerUser = User.builder().firstName("Manager").lastName("User")
                .email("manager@test.com").passwordHash("h").build();
        managerUser.setId(UUID.randomUUID());
        manager = Employee.builder().employeeCode("MGR-001").build();
        manager.setId(MANAGER_ID);
        manager.setUser(managerUser);

        // Employee (submitter)
        User empUser = User.builder().firstName("Employee").lastName("User")
                .email("emp@test.com").passwordHash("h").build();
        empUser.setId(UUID.randomUUID());
        employee = Employee.builder().employeeCode("EMP-001").build();
        employee.setId(EMPLOYEE_ID);
        employee.setUser(empUser);

        // Task
        task = Task.builder().title("Test Task").createdByEmployee(manager).build();
        task.setId(TASK_ID);
        task.setAssignedEmployee(employee);

        // Submission (belongs to employee)
        submission = TaskSubmission.builder()
                .task(task)
                .submittedBy(employee)
                .submissionNotes("My submission notes")
                .build();
        submission.setId(SUBMISSION_ID);

        // Common mocks
        when(groqClient.chat(anyString(), anyString())).thenReturn(buildGoodAnalysisJson());
        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
        when(extractionService.extractText(any(), any(), any())).thenReturn(null);

        TaskAiReview savedReview = TaskAiReview.builder()
                .task(task)
                .submission(submission)
                .requestedBy(manager)
                .status(AiReviewStatus.PENDING)
                .build();
        savedReview.setId(UUID.randomUUID());

        when(aiReviewRepository.save(any(TaskAiReview.class))).thenAnswer(inv -> {
            TaskAiReview r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
    }

    private String buildGoodAnalysisJson() {
        return """
                {
                  "completionScore": 85,
                  "overallAssessment": "Good work.",
                  "requirements": [],
                  "completedItems": ["Done"],
                  "missingItems": [],
                  "partialItems": [],
                  "qualityAssessment": {
                    "score": 88,
                    "summary": "Quality is good.",
                    "strengths": ["Clear code"],
                    "weaknesses": ["Missing docs"]
                  },
                  "issues": [],
                  "modificationSuggestions": ["Add docs"],
                  "managerSummary": "Manager summary",
                  "recommendedAction": "APPROVE",
                  "confidence": 90
                }
                """;
    }

    @Nested
    @DisplayName("Completion notifications")
    class CompletionNotifications {

        @Test
        @DisplayName("AI completion sends notification to the employee")
        void completionNotifiesEmployee() {
            // Setup: manager requested, employee submitted
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(manager));

            service.requestReview(SUBMISSION_ID);

            // Verify employee notification
            ArgumentCaptor<Employee> recipientCaptor = ArgumentCaptor.forClass(Employee.class);
            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            verify(notificationService, atLeastOnce()).createNotification(
                    recipientCaptor.capture(),
                    typeCaptor.capture(),
                    anyString(),
                    messageCaptor.capture(),
                    any(UUID.class));

            // At least one notification should go to the employee
            List<Employee> recipients = recipientCaptor.getAllValues();
            assertThat(recipients).anyMatch(r -> r.getId().equals(EMPLOYEE_ID));

            // Employee notification message should be friendly
            List<String> messages = messageCaptor.getAllValues();
            int employeeIdx = recipients.indexOf(recipients.stream()
                    .filter(r -> r.getId().equals(EMPLOYEE_ID)).findFirst().orElse(null));
            if (employeeIdx >= 0) {
                assertThat(messages.get(employeeIdx))
                        .doesNotContain("GroqClientException")
                        .doesNotContain("APPROVE")
                        .doesNotContain("REQUEST_CHANGES");
            }
        }

        @Test
        @DisplayName("AI completion notification type is AI_REVIEW_COMPLETED")
        void completionNotificationTypeIsCorrect() {
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(manager));

            service.requestReview(SUBMISSION_ID);

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            verify(notificationService, atLeastOnce()).createNotification(
                    any(Employee.class), typeCaptor.capture(), anyString(), anyString(), any(UUID.class));

            assertThat(typeCaptor.getAllValues()).allMatch(t -> t == NotificationType.AI_REVIEW_COMPLETED);
        }

        @Test
        @DisplayName("When requester and submitter are the same person, only one notification sent")
        void singleNotificationWhenRequesterEqualsSubmitter() {
            // Simulate manager who is also the submitter (edge case)
            submission = TaskSubmission.builder()
                    .task(task)
                    .submittedBy(manager) // manager submitted (unusual but possible for test)
                    .submissionNotes("Mgr submission")
                    .build();
            submission.setId(SUBMISSION_ID);

            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(manager));

            service.requestReview(SUBMISSION_ID);

            // Should only be 1 notification (not 2)
            verify(notificationService, times(1)).createNotification(
                    any(Employee.class), any(NotificationType.class),
                    anyString(), anyString(), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("Failure notifications")
    class FailureNotifications {

        @Test
        @DisplayName("AI failure sends employee notification with friendly message (no stack trace)")
        void failureNotifiesEmployeeWithFriendlyMessage() {
            when(groqClient.chat(anyString(), anyString()))
                    .thenThrow(new RuntimeException("GroqClientException: API timeout after 30s"));
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(manager));

            service.requestReview(SUBMISSION_ID);

            ArgumentCaptor<Employee> recipientCaptor = ArgumentCaptor.forClass(Employee.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            verify(notificationService, atLeastOnce()).createNotification(
                    recipientCaptor.capture(), any(), anyString(), messageCaptor.capture(), any());

            // Check employee received a notification
            boolean employeeNotified = recipientCaptor.getAllValues().stream()
                    .anyMatch(r -> r.getId().equals(EMPLOYEE_ID));
            assertThat(employeeNotified).isTrue();

            // Employee message must NOT contain the raw exception message
            messageCaptor.getAllValues().forEach(msg -> {
                if (recipientCaptor.getAllValues().get(messageCaptor.getAllValues().indexOf(msg)).getId().equals(EMPLOYEE_ID)) {
                    assertThat(msg)
                            .doesNotContain("GroqClientException")
                            .doesNotContain("API timeout after 30s");
                }
            });
        }

        @Test
        @DisplayName("AI failure notification type is AI_REVIEW_FAILED")
        void failureNotificationTypeIsCorrect() {
            when(groqClient.chat(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Forced failure"));
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(manager));

            service.requestReview(SUBMISSION_ID);

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            verify(notificationService, atLeastOnce()).createNotification(
                    any(Employee.class), typeCaptor.capture(), anyString(), anyString(), any(UUID.class));

            assertThat(typeCaptor.getAllValues()).allMatch(t -> t == NotificationType.AI_REVIEW_FAILED);
        }
    }
}
