package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Notification} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Returns a paginated list of notifications for a specific recipient,
     * ordered by creation time descending (newest first).
     *
     * @param recipientId the UUID of the recipient employee
     * @param pageable    pagination parameters
     * @return a page of notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :recipientId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientId(@Param("recipientId") UUID recipientId, Pageable pageable);

    /**
     * Counts unread notifications for a specific recipient.
     *
     * @param recipientId the UUID of the recipient employee
     * @return count of unread notifications
     */
    long countByRecipientIdAndReadFalse(UUID recipientId);

    /**
     * Marks a single notification as read, only if it belongs to the given recipient.
     *
     * @param notificationId the UUID of the notification
     * @param recipientId    the UUID of the recipient (ownership check)
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :notificationId AND n.recipient.id = :recipientId")
    int markAsRead(@Param("notificationId") UUID notificationId, @Param("recipientId") UUID recipientId);

    /**
     * Marks all unread notifications for a recipient as read.
     *
     * @param recipientId the UUID of the recipient employee
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    int markAllAsRead(@Param("recipientId") UUID recipientId);
}
