package com.company.employeemanagement.reviews;

import com.company.employeemanagement.dto.request.CreateReviewRequest;
import com.company.employeemanagement.dto.request.UpdateReviewRequest;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.ReviewResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.PerformanceReview;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.PerformanceReviewRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReviewServiceImpl}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl")
class ReviewServiceTest {

    @Mock private PerformanceReviewRepository reviewRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;

    private ReviewServiceImpl reviewService;

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID   = UUID.randomUUID();
    private static final UUID REVIEWER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository, employeeRepository, userRepository, securityUtils);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Department buildDept() {
        Department d = new Department();
        d.setName("Engineering");
        d.setCode("ENG");
        return d;
    }

    private Employee buildEmployee() {
        Employee e = Employee.builder()
                .employeeCode("EMP-001")
                .department(buildDept())
                .jobTitle("Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(new BigDecimal("60000"))
                .build();
        e.setId(EMPLOYEE_ID);
        return e;
    }

    private PerformanceReview buildReview(final Employee employee) {
        PerformanceReview r = PerformanceReview.builder()
                .employee(employee)
                .reviewerId(REVIEWER_ID)
                .reviewPeriod("Q1 2025")
                .rating(4)
                .reviewDate(LocalDate.of(2025, 3, 31))
                .comments("Good work")
                .goals("Improve test coverage")
                .build();
        r.setId(REVIEW_ID);
        return r;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // findAll
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("privileged user gets all reviews when no employeeId filter")
        void privilegedUserGetsAll() {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            Employee emp = buildEmployee();
            PerformanceReview review = buildReview(emp);
            Pageable pageable = PageRequest.of(0, 20);
            Page<PerformanceReview> page = new PageImpl<>(List.of(review), pageable, 1);
            when(reviewRepository.findAll(pageable)).thenReturn(page);
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            PageResponse<ReviewResponse> result = reviewService.findAll(null, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).reviewPeriod()).isEqualTo("Q1 2025");
        }

        @Test
        @DisplayName("privileged user can filter by employeeId")
        void privilegedFilterByEmployee() {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            Employee emp = buildEmployee();
            PerformanceReview review = buildReview(emp);
            Pageable pageable = PageRequest.of(0, 20);
            Page<PerformanceReview> page = new PageImpl<>(List.of(review), pageable, 1);
            when(reviewRepository.findByEmployeeId(EMPLOYEE_ID, pageable)).thenReturn(page);
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            PageResponse<ReviewResponse> result = reviewService.findAll(EMPLOYEE_ID, pageable);

            assertThat(result.content()).hasSize(1);
            verify(reviewRepository).findByEmployeeId(EMPLOYEE_ID, pageable);
        }

        @Test
        @DisplayName("EMPLOYEE principal only sees own reviews regardless of filter")
        void employeeSeesOwnOnly() {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            Employee own = buildEmployee();
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(own));
            PerformanceReview review = buildReview(own);
            Pageable pageable = PageRequest.of(0, 20);
            Page<PerformanceReview> page = new PageImpl<>(List.of(review), pageable, 1);
            when(reviewRepository.findByEmployeeId(EMPLOYEE_ID, pageable)).thenReturn(page);
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            PageResponse<ReviewResponse> result = reviewService.findAll(UUID.randomUUID(), pageable);

            assertThat(result.content()).hasSize(1);
            // regardless of the passed filter, own employee ID was used
            verify(reviewRepository).findByEmployeeId(EMPLOYEE_ID, pageable);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // findById
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns review for privileged user")
        void privilegedCanAccess() {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            Employee emp = buildEmployee();
            PerformanceReview review = buildReview(emp);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            ReviewResponse result = reviewService.findById(REVIEW_ID);

            assertThat(result.id()).isEqualTo(REVIEW_ID);
            assertThat(result.rating()).isEqualTo(4);
        }

        @Test
        @DisplayName("EMPLOYEE can access own review")
        void employeeCanAccessOwnReview() {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            Employee own = buildEmployee();
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(own));
            PerformanceReview review = buildReview(own);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            ReviewResponse result = reviewService.findById(REVIEW_ID);

            assertThat(result.employeeId()).isEqualTo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("EMPLOYEE cannot access another employee's review")
        void employeeCannotAccessOtherReview() {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            Employee own = buildEmployee();
            Employee other = buildEmployee();
            other.setId(UUID.randomUUID()); // different employee
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(own));
            PerformanceReview review = buildReview(other); // review belongs to other
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.findById(REVIEW_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own performance reviews");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown review ID")
        void unknownIdThrows404() {
            UUID unknown = UUID.randomUUID();
            when(reviewRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.findById(unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // create
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates review and sets reviewerId to current user")
        void createsWithReviewerId() {
            Employee emp = buildEmployee();
            when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(emp));
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(REVIEWER_ID));
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            PerformanceReview saved = buildReview(emp);
            when(reviewRepository.save(any(PerformanceReview.class))).thenReturn(saved);

            CreateReviewRequest req = new CreateReviewRequest(
                    EMPLOYEE_ID, "Q1 2025", 4, LocalDate.of(2025, 3, 31), "Good", "Goals");
            ReviewResponse result = reviewService.create(req);

            assertThat(result.rating()).isEqualTo(4);
            assertThat(result.reviewPeriod()).isEqualTo("Q1 2025");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void unknownEmployeeThrows() {
            UUID badId = UUID.randomUUID();
            when(employeeRepository.findById(badId)).thenReturn(Optional.empty());

            CreateReviewRequest req = new CreateReviewRequest(
                    badId, "Q1 2025", 4, LocalDate.of(2025, 3, 31), null, null);

            assertThatThrownBy(() -> reviewService.create(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // update
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("updates review fields")
        void updatesFields() {
            Employee emp = buildEmployee();
            PerformanceReview existing = buildReview(emp);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existing));
            when(userRepository.findById(any())).thenReturn(Optional.empty());
            when(reviewRepository.save(any(PerformanceReview.class))).thenAnswer(
                    inv -> inv.getArgument(0));

            UpdateReviewRequest req = new UpdateReviewRequest(
                    "Q2 2025", 5, LocalDate.of(2025, 6, 30), "Outstanding", "New goals");
            ReviewResponse result = reviewService.update(REVIEW_ID, req);

            assertThat(result.reviewPeriod()).isEqualTo("Q2 2025");
            assertThat(result.rating()).isEqualTo(5);
            assertThat(result.ratingLabel()).isEqualTo("Outstanding");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // delete
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deletes existing review")
        void deletesReview() {
            Employee emp = buildEmployee();
            PerformanceReview review = buildReview(emp);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            reviewService.delete(REVIEW_ID);

            verify(reviewRepository).delete(review);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown review")
        void unknownIdThrows() {
            UUID unknown = UUID.randomUUID();
            when(reviewRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.delete(unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // rating labels
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rating labels")
    class RatingLabels {

        @Test
        @DisplayName("rating 1 maps to Unsatisfactory")
        void rating1Label() {
            Employee emp = buildEmployee();
            PerformanceReview r = buildReview(emp);
            r.setRating(1);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(r));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            ReviewResponse result = reviewService.findById(REVIEW_ID);
            assertThat(result.ratingLabel()).isEqualTo("Unsatisfactory");
        }

        @Test
        @DisplayName("rating 5 maps to Outstanding")
        void rating5Label() {
            Employee emp = buildEmployee();
            PerformanceReview r = buildReview(emp);
            r.setRating(5);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(r));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            ReviewResponse result = reviewService.findById(REVIEW_ID);
            assertThat(result.ratingLabel()).isEqualTo("Outstanding");
        }
    }
}
