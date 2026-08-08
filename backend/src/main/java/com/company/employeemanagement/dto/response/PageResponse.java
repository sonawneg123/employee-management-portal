package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a paginated list of items along with pagination metadata.
 *
 * <p>Used by all paginated endpoints so that the client always receives
 * consistent navigation information alongside the data payload.
 *
 * @param <T>          the type of item contained in the page
 * @param content      the items on the current page
 * @param page         zero-based current page number
 * @param size         number of items requested per page
 * @param totalElements total count of matching items across all pages
 * @param totalPages   total number of pages available
 * @param last         {@code true} if this is the last page
 * @param timestamp    server timestamp when the response was produced
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Generic paginated response wrapper")
public record PageResponse<T>(

        @Schema(description = "Items on the current page")
        List<T> content,

        @Schema(description = "Zero-based current page number", example = "0")
        int page,

        @Schema(description = "Number of items per page", example = "20")
        int size,

        @Schema(description = "Total number of matching items", example = "150")
        long totalElements,

        @Schema(description = "Total number of pages available", example = "8")
        int totalPages,

        @Schema(description = "Whether this is the last page", example = "false")
        boolean last,

        @Schema(description = "Server timestamp when the response was produced")
        LocalDateTime timestamp
) {

    /**
     * Convenience factory method that builds a {@link PageResponse} from a
     * Spring Data {@link org.springframework.data.domain.Page}.
     *
     * @param <T>        the element type
     * @param springPage the Spring Data page to wrap
     * @return a populated {@link PageResponse}
     */
    public static <T> PageResponse<T> from(final org.springframework.data.domain.Page<? extends T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast(),
                LocalDateTime.now()
        );
    }
}
