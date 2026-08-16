package com.company.employeemanagement.service;

import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Scheduled service that sends deadline reminder notifications for tasks.
 *
 * <p>Runs every hour. For each non-completed task with a due date:
 * <ul>
 *   <li>If the due date is tomorrow and the 24h reminder has not been sent → send and flag.</li>
 *   <li>If the due date is today and ≤ 2 hours remain and the 2h reminder has not been sent → send and flag.</li>
 *   <li>If the due date is in the past and the overdue notification has not been sent → send and flag.</li>
 * </ul>
 *
 * <p>Deduplication is handled via boolean columns on the {@link Task} entity
 * ({@code reminder24hSent}, {@code reminder2hSent}, {@code overdueNotificationSent}).
 *
 * @author Employee Management Portal Team
 */
@Service
public class TaskDeadlineReminderService {

    private static final Logger log = LoggerFactory.getLogger(TaskDeadlineReminderService.class);

    private static final List<TaskStatus> EXCLUDED_STATUSES =
            List.of(TaskStatus.COMPLETED, TaskStatus.REJECTED);

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public TaskDeadlineReminderService(final TaskRepository taskRepository,
                                        final NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    /**
     * Runs every hour on the hour (e.g., 09:00, 10:00, …).
     * Scans all non-completed tasks with a due date and sends reminders as needed.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendDeadlineReminders() {
        log.info("TaskDeadlineReminder: starting scheduled run at {}", LocalDateTime.now());

        List<Task> tasks = taskRepository.findNonCompletedTasksWithDueDate(EXCLUDED_STATUSES);
        log.debug("TaskDeadlineReminder: evaluating {} tasks", tasks.size());

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        int sent24h = 0, sent2h = 0, sentOverdue = 0;

        for (Task task : tasks) {
            if (task.getAssignedEmployee() == null) continue;

            try {
                LocalDate dueDate = task.getDueDate();

                // ── Overdue notification ──────────────────────────────────────
                if (dueDate.isBefore(today) && !task.isOverdueNotificationSent()) {
                    notificationService.createNotification(
                            task.getAssignedEmployee(),
                            NotificationType.TASK_OVERDUE,
                            "Task Overdue",
                            "Your task \"" + task.getTitle() + "\" was due on "
                                    + dueDate + " and is now overdue.",
                            task.getId()
                    );
                    task.setOverdueNotificationSent(true);
                    taskRepository.save(task);
                    sentOverdue++;
                    continue; // no need to check other reminders if already overdue
                }

                // ── 24h reminder (due tomorrow) ──────────────────────────────
                if (dueDate.equals(today.plusDays(1)) && !task.isReminder24hSent()) {
                    notificationService.createNotification(
                            task.getAssignedEmployee(),
                            NotificationType.TASK_DUE_SOON,
                            "Task Due Tomorrow",
                            "Reminder: your task \"" + task.getTitle()
                                    + "\" is due tomorrow (" + dueDate + ").",
                            task.getId()
                    );
                    task.setReminder24hSent(true);
                    taskRepository.save(task);
                    sent24h++;
                }

                // ── 2h reminder (due today, ≤ 2 hours remaining) ─────────────
                if (dueDate.equals(today) && !task.isReminder2hSent()) {
                    LocalDateTime dueDateTime = LocalDateTime.of(dueDate, LocalTime.of(17, 0)); // end of business day
                    long minutesRemaining = java.time.Duration.between(now, dueDateTime).toMinutes();
                    if (minutesRemaining >= 0 && minutesRemaining <= 120) {
                        notificationService.createNotification(
                                task.getAssignedEmployee(),
                                NotificationType.TASK_DUE_SOON,
                                "Task Due in 2 Hours",
                                "Reminder: your task \"" + task.getTitle()
                                        + "\" is due today within the next 2 hours.",
                                task.getId()
                        );
                        task.setReminder2hSent(true);
                        taskRepository.save(task);
                        sent2h++;
                    }
                }

            } catch (Exception e) {
                log.warn("TaskDeadlineReminder: error processing task id={}: {}",
                        task.getId(), e.getMessage());
            }
        }

        log.info("TaskDeadlineReminder: done — 24h={}, 2h={}, overdue={}",
                sent24h, sent2h, sentOverdue);
    }
}
