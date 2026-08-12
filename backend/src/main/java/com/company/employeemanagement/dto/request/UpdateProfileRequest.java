package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating the authenticated user's own personal information.
 *
 * <p>All fields are optional — only non-null values are applied to the record.
 * This allows the profile page to update only phone/address without requiring
 * the caller to re-supply firstName and lastName.
 *
 * @param firstName updated first name (optional — kept unchanged if null)
 * @param lastName  updated last name  (optional — kept unchanged if null)
 * @param phone     updated phone number (optional)
 * @param address   updated address (optional)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for updating the authenticated user's personal information")
public record UpdateProfileRequest(

        @Schema(description = "Updated first name", example = "Jane")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Updated last name", example = "Smith")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Schema(description = "Updated phone number", example = "+1-555-0123")
        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Schema(description = "Updated address", example = "456 Oak Ave")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {
}
