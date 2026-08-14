package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email delivery implementation using Spring's {@link JavaMailSender}.
 *
 * <p>Sends HTML-formatted password-reset OTP emails. The OTP itself is
 * included in the email body but <strong>never</strong> written to application logs.
 *
 * <p>All SMTP configuration is injected from {@code app.mail.*} properties —
 * nothing is hard-coded.
 *
 * @author Employee Management Portal Team
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    /**
     * The "from" address shown in the email.
     * Bound from {@code app.mail.from} (e.g. {@code noreply@company.com}).
     */
    @Value("${app.mail.from:noreply@company.com}")
    private String fromAddress;

    /**
     * Application name shown in the email subject and body.
     * Bound from {@code app.mail.app-name}.
     */
    @Value("${app.mail.app-name:PeopleCore HR}")
    private String appName;

    /**
     * Constructs the service with the required mail sender dependency.
     *
     * @param mailSender the Spring mail sender
     */
    public EmailServiceImpl(final JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends an HTML email containing the OTP, expiry information,
     * and a security warning. The OTP is <strong>not</strong> logged.
     */
    @Override
    public void sendPasswordResetOtp(final String toEmail,
                                     final String recipientName,
                                     final String otp,
                                     final int expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("[" + appName + "] Your Password Reset Code");
            helper.setText(buildHtmlBody(recipientName, otp, expiryMinutes), true);

            mailSender.send(message);
            // Log delivery without exposing the OTP
            log.info("Password-reset OTP email sent to {}", toEmail);
        } catch (MessagingException | MailException ex) {
            // Log at error level but rethrow so the caller can surface the failure
            log.error("Failed to send password-reset OTP to {}: {}", toEmail, ex.getMessage());
            throw new RuntimeException("Failed to send password-reset email. Please try again later.", ex);
        }
    }

    // ── private helpers ────────────────────────────────────────────────────────

    /**
     * Builds the HTML email body for a password-reset OTP.
     *
     * @param recipientName the user's display name
     * @param otp           the raw OTP (included in the email, never in logs)
     * @param expiryMinutes OTP validity period
     * @return HTML string
     */
    private String buildHtmlBody(final String recipientName,
                                  final String otp,
                                  final int expiryMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Password Reset OTP</title>
                </head>
                <body style="font-family: -apple-system, 'Segoe UI', Arial, sans-serif;
                             background: #f7f8fa; margin: 0; padding: 32px 16px;">
                  <div style="max-width: 480px; margin: 0 auto; background: #ffffff;
                              border-radius: 12px; border: 1px solid #e5e7eb;
                              overflow: hidden;">
                    <!-- Header -->
                    <div style="background: linear-gradient(135deg, #3730A3 0%, #4F46E5 100%);
                                padding: 28px 32px;">
                      <h1 style="color: #ffffff; margin: 0; font-size: 20px; font-weight: 700;">
                """ + escapeHtml(appName) + """
                      </h1>
                    </div>
                    <!-- Body -->
                    <div style="padding: 32px;">
                      <p style="color: #1f2328; font-size: 15px; margin: 0 0 16px;">
                        Hello <strong>""" + escapeHtml(recipientName) + """
                </strong>,
                      </p>
                      <p style="color: #57606a; font-size: 14px; margin: 0 0 24px; line-height: 1.6;">
                        We received a request to reset the password for your account.
                        Use the code below to complete the process.
                        This code expires in <strong>""" + expiryMinutes + """
                 minutes</strong>.
                      </p>
                      <!-- OTP box -->
                      <div style="background: #f0f4ff; border: 2px solid #4F46E5;
                                  border-radius: 10px; padding: 20px 24px;
                                  text-align: center; margin-bottom: 24px;">
                        <p style="color: #57606a; font-size: 12px; margin: 0 0 8px;
                                  text-transform: uppercase; letter-spacing: 1px; font-weight: 600;">
                          Your OTP Code
                        </p>
                        <p style="color: #3730A3; font-size: 36px; font-weight: 800;
                                  letter-spacing: 8px; margin: 0; font-family: monospace;">
                """ + escapeHtml(otp) + """
                        </p>
                      </div>
                      <!-- Security warning -->
                      <div style="background: #fff8f0; border-left: 4px solid #f59e0b;
                                  border-radius: 4px; padding: 12px 16px; margin-bottom: 24px;">
                        <p style="color: #92400e; font-size: 13px; margin: 0; line-height: 1.5;">
                          ⚠️ <strong>Security notice:</strong> If you did not request a password reset,
                          please ignore this email. Do not share this code with anyone.
                          Our team will never ask for your OTP.
                        </p>
                      </div>
                      <p style="color: #57606a; font-size: 13px; margin: 0;">
                        If you have any questions, please contact support.
                      </p>
                    </div>
                    <!-- Footer -->
                    <div style="border-top: 1px solid #e5e7eb; padding: 16px 32px;
                                background: #f7f8fa;">
                      <p style="color: #57606a; font-size: 12px; margin: 0; text-align: center;">
                        © """ + escapeHtml(appName) + """
                 · This is an automated message, please do not reply.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """;
    }

    /**
     * Minimally escapes HTML special characters to prevent XSS in email bodies.
     *
     * @param input the raw string
     * @return the HTML-escaped string
     */
    private static String escapeHtml(final String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
