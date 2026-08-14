package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.ForgotPasswordRequest;
import com.company.employeemanagement.dto.request.ResetPasswordRequest;
import com.company.employeemanagement.dto.request.VerifyOtpRequest;
import com.company.employeemanagement.dto.response.MessageResponse;

/**
 * Service contract for the password-reset OTP flow.
 *
 * <p>The three-step flow:
 * <ol>
 *   <li>{@link #requestPasswordReset(ForgotPasswordRequest)} — generates and emails an OTP.</li>
 *   <li>{@link #verifyOtp(VerifyOtpRequest)} — verifies the OTP and marks the token as verified.</li>
 *   <li>{@link #resetPassword(ResetPasswordRequest)} — exchanges the verified token for a new password.</li>
 * </ol>
 *
 * @author Employee Management Portal Team
 */
public interface PasswordResetService {

    /**
     * Initiates the password-reset flow for the given email address.
     *
     * <p>If the email is registered, a cryptographically secure 6-digit OTP is
     * generated, hashed, persisted, and emailed to the user.
     *
     * <p><strong>Security</strong>: the same generic response is returned whether
     * or not the email exists, to prevent email-existence enumeration.
     *
     * @param request the request carrying the email address
     * @return a generic {@link MessageResponse}
     */
    MessageResponse requestPasswordReset(ForgotPasswordRequest request);

    /**
     * Verifies the OTP provided by the user.
     *
     * <p>On success, the token is marked as verified. A subsequent call to
     * {@link #resetPassword(ResetPasswordRequest)} within the same expiry window
     * can then set the new password.
     *
     * @param request the email + OTP pair
     * @return a success {@link MessageResponse}
     * @throws IllegalArgumentException if the OTP is incorrect, expired, or
     *                                   the maximum number of attempts has been reached
     */
    MessageResponse verifyOtp(VerifyOtpRequest request);

    /**
     * Resets the user's password using a previously verified OTP token.
     *
     * <p>The verified token is consumed (marked as used) so it cannot be
     * reused. The new password is hashed with BCrypt before persistence.
     *
     * @param request the email + new password + confirmation
     * @return a success {@link MessageResponse}
     * @throws IllegalArgumentException if the new passwords do not match, or if
     *                                   no valid verified token exists for the email
     */
    MessageResponse resetPassword(ResetPasswordRequest request);
}
