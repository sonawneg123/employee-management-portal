package com.company.employeemanagement.service;

/**
 * Service contract for sending transactional emails.
 *
 * <p>The implementation uses Spring's {@code JavaMailSender} configured via
 * {@code app.mail.*} properties. A no-op test implementation is provided
 * for unit tests.
 *
 * @author Employee Management Portal Team
 */
public interface EmailService {

    /**
     * Sends a password-reset OTP to the specified email address.
     *
     * @param toEmail         the recipient's email address
     * @param recipientName   the recipient's display name (used in the email body)
     * @param otp             the raw (unhashed) 6-digit OTP
     * @param expiryMinutes   the number of minutes until the OTP expires
     */
    void sendPasswordResetOtp(String toEmail, String recipientName, String otp, int expiryMinutes);
}
