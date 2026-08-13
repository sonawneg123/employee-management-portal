package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateReviewRequest;
import com.company.employeemanagement.dto.request.UpdateReviewRequest;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.ReviewResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.PerformanceReview;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.PerformanceReviewRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link ReviewService}.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>ADMIN, HR, MANAGER — may view all reviews or filter by employee;
 *       may create and update reviews.</li>
 *   <li>ADMIN — additionally may delete reviews.</li>
 *   <li>EMPLOYEE — may only view reviews belonging to their own employee record;
 *       cannot create, update, or delete reviews.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String[] RATING_LABELS = {
        "", "Unsatisfactory", "Needs Improvement", "Meets Expectations", "Good", "Outstanding"
    };

    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Constructs the service with required dependencies.
     *
     * @param reviewRepository   repository for performance review persistence
     * @param employeeRepository repository for employee lookups
     * @param userRepository     repository for reviewer name lookups
     * @param securityUtils      helper for current-principal inspection
     */
    public ReviewServiceImpl(final PerformanceReviewRepository reviewRepository,
                              final EmployeeRepository employeeRepository,
                              final UserRepository userRepository,
                              final SecurityUtils securityUtils) {
        this.reviewRepository = reviewRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the caller is EMPLOYEE, the {@code employeeId} filter is automatically
     * overridden to the caller's own employee record regardless of what was passed.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> findAll(final UUID employeeId, final Pageable pageable) {
        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            // EMPLOYEE: force scope to own reviews; return empty page if no linked record yet
            java.util.Optional<Employee> maybeEmp = securityUtils.getCurrentEmployee();
            if (maybeEmp.isEmpty()) {
                return PageResponse.from(
                        new org.springframework.data.domain.PageImpl<>(
                                java.util.List.of(), pageable, 0L));
            }
            Page<PerformanceReview> page = reviewRepository.findByEmployeeId(maybeEmp.get().getId(), pageable);
            return PageResponse.from(page.map(this::toResponse));
        }

        // Privileged: optionally filter by employeeId
        Page<PerformanceReview> page;
        if (employeeId != null) {
            page = reviewRepository.findByEmployeeId(employeeId, pageable);
        } else {
            page = reviewRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    /**
     * {@inheritDoc}
     *
     * <p>EMPLOYEE principals are blocked from accessing reviews belonging to a
     * different employee.
     */
    @Override
    @Transactional(readOnly = true)
    public ReviewResponse findById(final UUID id) {
        PerformanceReview review = loadReview(id);

        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            Employee own = resolveCurrentEmployee();
            if (!review.getEmployee().getId().equals(own.getId())) {
                throw new AccessDeniedException(
                        "You may only access your own performance reviews.");
            }
        }

        return toResponse(review);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The reviewer is automatically set to the currently authenticated user.
     */
    @Override
    @Transactional
    public ReviewResponse create(final CreateReviewRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.employeeId()));

        UUID reviewerId = securityUtils.getCurrentUserId().orElse(null);

        PerformanceReview review = PerformanceReview.builder()
                .employee(employee)
                .reviewerId(reviewerId)
                .reviewPeriod(request.reviewPeriod())
                .rating(request.rating())
                .reviewDate(request.reviewDate())
                .comments(request.comments())
                .goals(request.goals())
                .build();

        return toResponse(reviewRepository.save(review));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ReviewResponse update(final UUID id, final UpdateReviewRequest request) {
        PerformanceReview review = loadReview(id);

        review.setReviewPeriod(request.reviewPeriod());
        review.setRating(request.rating());
        review.setReviewDate(request.reviewDate());
        review.setComments(request.comments());
        review.setGoals(request.goals());

        return toResponse(reviewRepository.save(review));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(final UUID id) {
        PerformanceReview review = loadReview(id);
        reviewRepository.delete(review);
    }

    // ────────────────────────── private helpers ────────────────────────────────

    private PerformanceReview loadReview(final UUID id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PerformanceReview", id));
    }

    private Employee resolveCurrentEmployee() {
        return securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account."));
    }

    private ReviewResponse toResponse(final PerformanceReview review) {
        Employee emp = review.getEmployee();
        String employeeName = null;
        if (emp.getUser() != null) {
            employeeName = emp.getUser().getFirstName() + " " + emp.getUser().getLastName();
        } else if (emp.getFirstName() != null) {
            // HR-created employees have no User account — use the entity's own name fields
            employeeName = emp.getFirstName()
                    + (emp.getLastName() != null ? " " + emp.getLastName() : "");
        }

        String reviewerName = null;
        if (review.getReviewerId() != null) {
            Optional<User> reviewer = userRepository.findById(review.getReviewerId());
            if (reviewer.isPresent()) {
                reviewerName = reviewer.get().getFirstName() + " " + reviewer.get().getLastName();
            }
        }

        int rating = review.getRating();
        String ratingLabel = (rating >= 1 && rating <= 5) ? RATING_LABELS[rating] : "Unknown";

        return new ReviewResponse(
                review.getId(),
                emp.getId(),
                emp.getEmployeeCode(),
                employeeName,
                emp.getDepartment() != null ? emp.getDepartment().getName() : null,
                review.getReviewerId(),
                reviewerName,
                review.getReviewPeriod(),
                rating,
                ratingLabel,
                review.getReviewDate(),
                review.getComments(),
                review.getGoals(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getCreatedBy(),
                review.getUpdatedBy()
        );
    }
}
