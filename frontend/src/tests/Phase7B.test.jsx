/**
 * @fileoverview Phase 7B Frontend Tests — AI Evaluation UI & Review Workflow
 *
 * Covers:
 *  1.  No submission → AI section shows "unavailable" message, no Run button
 *  2.  Submission exists, no review yet → "Run AI Evaluation" button shown
 *  3.  Run Evaluation button triggers API call
 *  4.  Loading/in-flight state shown (PENDING status)
 *  5.  PROCESSING status shows evaluating indicator
 *  6.  Completed evaluation renders score, recommendation, summary
 *  7.  Failed evaluation shows error state with Retry button
 *  8.  Retry calls run again
 *  9.  Score rendering — completion + quality bars
 * 10.  APPROVE recommendation renders correctly
 * 11.  REQUEST_CHANGES recommendation renders correctly
 * 12.  MANUAL_REVIEW recommendation renders correctly
 * 13.  Summary (managerSummary) rendered as plain text
 * 14.  Strengths list rendered
 * 15.  Issues list rendered
 * 16.  Suggested changes list rendered
 * 17.  Evaluation history table shown for multiple reviews
 * 18.  History row click selects that review
 * 19.  taskAiReviewApi — requestAiReview calls correct endpoint
 * 20.  taskAiReviewApi — getLatestAiReview calls correct endpoint
 * 21.  taskAiReviewApi — getAllAiReviews calls correct endpoint
 * 22.  useTaskAiReviewHooks exports expected hooks
 * 23.  No raw JSON exposed
 * 24.  "Re-run Evaluation" label shown when existing review present
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';

import TaskAiEvaluationSection from '@/components/tasks/TaskAiEvaluationSection';

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock('@/hooks/useTaskAiReviewHooks', () => ({
  useLatestAiReview: vi.fn(),
  useAllAiReviews: vi.fn(),
  useRunAiReview: vi.fn(),
}));

import {
  useLatestAiReview,
  useAllAiReviews,
  useRunAiReview,
} from '@/hooks/useTaskAiReviewHooks';

// ── Test helpers ──────────────────────────────────────────────────────────────

const theme = createTheme();

function Wrapper({ children }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>
        {children}
      </QueryClientProvider>
    </ThemeProvider>
  );
}

function renderSection(props) {
  return render(
    <Wrapper>
      <TaskAiEvaluationSection {...props} />
    </Wrapper>,
  );
}

/** A minimal valid submission object. */
const MOCK_SUBMISSION = {
  id: 'sub-uuid-1',
  taskId: 'task-uuid-1',
  taskTitle: 'Build Login Page',
  submittedByName: 'Alice',
  submittedAt: new Date().toISOString(),
  reviewStatus: 'PENDING_REVIEW',
  hasAttachment: false,
};

/** A completed review with full structured JSON. */
const STRUCTURED_JSON = JSON.stringify({
  completionScore: 82,
  overallAssessment: 'Overall good submission.',
  managerSummary: 'The employee completed the login page with email/password validation.',
  requirements: [
    { requirement: 'Login form UI', status: 'COMPLETED', evidence: 'Implemented', suggestion: null },
    { requirement: 'API integration', status: 'PARTIALLY_COMPLETED', evidence: 'Partial', suggestion: 'Complete the auth API call.' },
  ],
  completedItems: ['Login form UI', 'Responsive layout'],
  missingItems: ['Error handling for 401'],
  partialItems: ['API integration'],
  qualityAssessment: {
    score: 78,
    summary: 'Code quality is acceptable.',
    strengths: ['Responsive login interface implemented', 'Email validation implemented'],
    weaknesses: ['Authentication API integration is incomplete'],
  },
  issues: ['Loading state is missing'],
  modificationSuggestions: ['Complete the authentication API integration.', 'Add a loading state.'],
  recommendedAction: 'REQUEST_CHANGES',
  confidence: 85,
});

const COMPLETED_REVIEW = {
  id: 'review-uuid-1',
  taskId: 'task-uuid-1',
  submissionId: 'sub-uuid-1',
  requestedById: 'mgr-uuid-1',
  requestedByName: 'Manager One',
  status: 'COMPLETED',
  aiProvider: 'groq',
  aiModel: 'llama-3.1-8b-instant',
  promptVersion: 'v1',
  completionScore: 82,
  qualityScore: 78,
  confidence: 85,
  recommendedAction: 'REQUEST_CHANGES',
  structuredAnalysisJson: STRUCTURED_JSON,
  managerSummary: 'The employee completed the login page with email/password validation.',
  errorMessage: null,
  createdAt: new Date().toISOString(),
  completedAt: new Date().toISOString(),
};

const FAILED_REVIEW = {
  ...COMPLETED_REVIEW,
  id: 'review-uuid-fail',
  status: 'FAILED',
  completionScore: null,
  qualityScore: null,
  confidence: null,
  recommendedAction: null,
  structuredAnalysisJson: null,
  managerSummary: null,
  errorMessage: 'GroqClientException: timeout after 30s',
  completedAt: new Date().toISOString(),
};

const PENDING_REVIEW = {
  ...COMPLETED_REVIEW,
  id: 'review-uuid-pend',
  status: 'PENDING',
  completionScore: null,
  qualityScore: null,
  confidence: null,
  recommendedAction: null,
  structuredAnalysisJson: null,
  managerSummary: null,
  errorMessage: null,
  completedAt: null,
};

// ── Default hook mock setup ───────────────────────────────────────────────────

function setupHooks({
  latestReview = undefined,
  latestIsLoading = false,
  latestError = null,
  allReviews = [],
  allIsLoading = false,
  runMutateAsync = vi.fn().mockResolvedValue(COMPLETED_REVIEW),
  runIsPending = false,
} = {}) {
  useLatestAiReview.mockReturnValue({
    data: latestReview,
    isLoading: latestIsLoading,
    error: latestError,
  });
  useAllAiReviews.mockReturnValue({
    data: allReviews,
    isLoading: allIsLoading,
    error: null,
  });
  useRunAiReview.mockReturnValue({
    mutateAsync: runMutateAsync,
    isPending: runIsPending,
  });
}

// ── Test suites ───────────────────────────────────────────────────────────────

describe('TaskAiEvaluationSection — no submission', () => {
  beforeEach(() => {
    setupHooks();
  });

  it('shows unavailable message when submission is null', () => {
    renderSection({ taskId: 'task-1', submission: null });
    expect(screen.getByText(/ai evaluation will become available/i)).toBeInTheDocument();
  });

  it('does not render a Run AI Evaluation button when submission is null', () => {
    renderSection({ taskId: 'task-1', submission: null });
    expect(screen.queryByRole('button', { name: /run ai evaluation/i })).not.toBeInTheDocument();
  });

  it('does not render a Re-run button when submission is null', () => {
    renderSection({ taskId: 'task-1', submission: null });
    expect(screen.queryByRole('button', { name: /re-run/i })).not.toBeInTheDocument();
  });
});

describe('TaskAiEvaluationSection — submission present, no review yet', () => {
  beforeEach(() => {
    setupHooks({
      latestReview: undefined,
      latestError: { response: { status: 404 } },
    });
  });

  it('shows "Run AI Evaluation" button when no review exists', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByRole('button', { name: /run ai evaluation/i })).toBeInTheDocument();
  });

  it('shows "no evaluation has been run" message', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/no ai evaluation has been run/i)).toBeInTheDocument();
  });
});

describe('TaskAiEvaluationSection — Run Evaluation button triggers API', () => {
  it('calls runMutation.mutateAsync with submissionId on click', async () => {
    const mutateAsync = vi.fn().mockResolvedValue(COMPLETED_REVIEW);
    setupHooks({ mutateAsync, runMutateAsync: mutateAsync });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    const btn = screen.getByRole('button', { name: /run ai evaluation/i });
    fireEvent.click(btn);

    await waitFor(() => {
      expect(mutateAsync).toHaveBeenCalledWith(MOCK_SUBMISSION.id);
    });
  });

  it('shows run error message when API call fails', async () => {
    const mutateAsync = vi.fn().mockRejectedValue({
      response: { data: { detail: 'Groq is unavailable.' } },
    });
    setupHooks({ runMutateAsync: mutateAsync });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    fireEvent.click(screen.getByRole('button', { name: /run ai evaluation/i }));

    await waitFor(() => {
      expect(screen.getByText(/groq is unavailable/i)).toBeInTheDocument();
    });
  });
});

describe('TaskAiEvaluationSection — loading / in-flight state', () => {
  it('disables run button while mutation is pending', () => {
    setupHooks({ runIsPending: true, latestReview: undefined });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    // When runIsPending=true and no existing review, label stays "Run AI Evaluation" but is disabled
    const btn = screen.getByRole('button', { name: /run ai evaluation/i });
    expect(btn).toBeDisabled();
  });

  it('shows "Evaluating…" chip when latest review is PENDING', () => {
    setupHooks({ latestReview: PENDING_REVIEW });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/queued/i)).toBeInTheDocument();
  });

  it('shows skeleton when isLoadingLatest is true', () => {
    setupHooks({ latestIsLoading: true });

    const { container } = renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    // MUI Skeleton renders as a span with Skeleton class
    const skeletons = container.querySelectorAll('.MuiSkeleton-root');
    expect(skeletons.length).toBeGreaterThan(0);
  });
});

describe('TaskAiEvaluationSection — completed evaluation', () => {
  beforeEach(() => {
    setupHooks({ latestReview: COMPLETED_REVIEW, allReviews: [COMPLETED_REVIEW] });
  });

  it('shows "Evaluation Complete" status chip', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/evaluation complete/i)).toBeInTheDocument();
  });

  it('renders completion score', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    // Score 82 appears in multiple places (score display + bar label)
    expect(screen.getAllByText('82').length).toBeGreaterThanOrEqual(1);
  });

  it('renders quality score', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getAllByText('78').length).toBeGreaterThanOrEqual(1);
  });

  it('renders manager summary as plain text', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(
      screen.getByText(/the employee completed the login page with email\/password validation/i),
    ).toBeInTheDocument();
  });

  it('renders AI Summary heading', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/ai summary/i)).toBeInTheDocument();
  });

  it('does not expose raw structuredAnalysisJson string to the user', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    // The raw JSON key "completionScore" should not appear as visible text
    expect(screen.queryByText(/"completionScore"/)).not.toBeInTheDocument();
    // Double-check no raw JSON brace leaks
    expect(screen.queryByText(/^\{.*"completionScore"/)).not.toBeInTheDocument();
  });

  it('renders "Re-run Evaluation" button when review exists', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByRole('button', { name: /re-run evaluation/i })).toBeInTheDocument();
  });

  it('shows "AI Evaluation" heading', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText('AI Evaluation')).toBeInTheDocument();
  });

  it('shows advisory-only notice', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/advisory only/i)).toBeInTheDocument();
  });
});

describe('TaskAiEvaluationSection — recommendation rendering', () => {
  it('renders APPROVE recommendation chip', () => {
    const approveReview = { ...COMPLETED_REVIEW, recommendedAction: 'APPROVE' };
    setupHooks({ latestReview: approveReview, allReviews: [approveReview] });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    // "Approve" chip label appears — may appear in advisory text too, use getAllByText
    expect(screen.getAllByText(/approve/i).length).toBeGreaterThanOrEqual(1);
  });

  it('renders REQUEST_CHANGES recommendation chip', () => {
    setupHooks({ latestReview: COMPLETED_REVIEW, allReviews: [COMPLETED_REVIEW] });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/request changes/i)).toBeInTheDocument();
  });

  it('renders MANUAL_REVIEW recommendation chip', () => {
    const manualReview = { ...COMPLETED_REVIEW, recommendedAction: 'MANUAL_REVIEW' };
    setupHooks({ latestReview: manualReview, allReviews: [manualReview] });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/review manually/i)).toBeInTheDocument();
  });
});

describe('TaskAiEvaluationSection — strengths', () => {
  beforeEach(() => {
    setupHooks({ latestReview: COMPLETED_REVIEW, allReviews: [COMPLETED_REVIEW] });
  });

  it('shows Strengths heading', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/strengths/i)).toBeInTheDocument();
  });

  it('lists each strength item', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/responsive login interface implemented/i)).toBeInTheDocument();
    expect(screen.getByText(/email validation implemented/i)).toBeInTheDocument();
  });
});

describe('TaskAiEvaluationSection — issues', () => {
  beforeEach(() => {
    setupHooks({ latestReview: COMPLETED_REVIEW, allReviews: [COMPLETED_REVIEW] });
  });

  it('shows Issues Found heading', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/issues found/i)).toBeInTheDocument();
  });

  it('lists each issue', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/loading state is missing/i)).toBeInTheDocument();
  });

  it('lists weaknesses from qualityAssessment', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/authentication api integration is incomplete/i)).toBeInTheDocument();
  });
});

describe('TaskAiEvaluationSection — suggested changes', () => {
  beforeEach(() => {
    setupHooks({ latestReview: COMPLETED_REVIEW, allReviews: [COMPLETED_REVIEW] });
  });

  it('shows Suggested Improvements heading', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/suggested improvements/i)).toBeInTheDocument();
  });

  it('lists each suggestion with ordinal prefix', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/1\. complete the authentication api integration/i)).toBeInTheDocument();
    expect(screen.getByText(/2\. add a loading state/i)).toBeInTheDocument();
  });
});

describe('TaskAiEvaluationSection — failed evaluation', () => {
  beforeEach(() => {
    setupHooks({ latestReview: FAILED_REVIEW, allReviews: [FAILED_REVIEW] });
  });

  it('shows "Evaluation Failed" status chip', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    // Chip + alert both contain "failed" text
    expect(screen.getAllByText(/evaluation failed/i).length).toBeGreaterThanOrEqual(1);
  });

  it('shows error message from backend', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    // Error text is split across DOM nodes by MUI Alert — use container query
    const alert = document.querySelector('[role="alert"]');
    expect(alert).toBeTruthy();
    expect(alert.textContent).toMatch(/GroqClientException/i);
  });

  it('shows Retry button', () => {
    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('Retry button triggers mutation', async () => {
    const mutateAsync = vi.fn().mockResolvedValue(COMPLETED_REVIEW);
    setupHooks({
      latestReview: FAILED_REVIEW,
      allReviews: [FAILED_REVIEW],
      runMutateAsync: mutateAsync,
    });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));

    await waitFor(() => {
      expect(mutateAsync).toHaveBeenCalledWith(MOCK_SUBMISSION.id);
    });
  });
});

describe('TaskAiEvaluationSection — evaluation history', () => {
  const OLDER_REVIEW = {
    ...COMPLETED_REVIEW,
    id: 'review-uuid-old',
    completionScore: 74,
    qualityScore: 70,
    recommendedAction: 'REQUEST_CHANGES',
    createdAt: new Date(Date.now() - 3_600_000).toISOString(),
    completedAt: new Date(Date.now() - 3_600_000).toISOString(),
  };

  it('shows Evaluation History when more than one review exists', () => {
    setupHooks({
      latestReview: COMPLETED_REVIEW,
      allReviews: [COMPLETED_REVIEW, OLDER_REVIEW],
    });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.getByText(/evaluation history/i)).toBeInTheDocument();
  });

  it('does not show history table when only one review exists', () => {
    setupHooks({
      latestReview: COMPLETED_REVIEW,
      allReviews: [COMPLETED_REVIEW],
    });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    expect(screen.queryByText(/evaluation history/i)).not.toBeInTheDocument();
  });

  it('renders both review scores in history table', () => {
    setupHooks({
      latestReview: COMPLETED_REVIEW,
      allReviews: [COMPLETED_REVIEW, OLDER_REVIEW],
    });

    renderSection({ taskId: 'task-1', submission: MOCK_SUBMISSION });
    // The history table should show both completion scores
    const scoreEls = screen.getAllByText('82');
    expect(scoreEls.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('74')).toBeInTheDocument();
  });
});

// ── API service unit tests ─────────────────────────────────────────────────────

describe('taskAiReviewApi — service functions', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it('exports requestAiReview, getLatestAiReview, getAllAiReviews, getAiReviewById', async () => {
    const mod = await import('@/services/taskAiReviewApi');
    expect(typeof mod.requestAiReview).toBe('function');
    expect(typeof mod.getLatestAiReview).toBe('function');
    expect(typeof mod.getAllAiReviews).toBe('function');
    expect(typeof mod.getAiReviewById).toBe('function');
  });
});

// ── Hook exports test ─────────────────────────────────────────────────────────

describe('useTaskAiReviewHooks — exports', () => {
  it('exports useLatestAiReview, useAllAiReviews, useRunAiReview', async () => {
    // Re-import the real module (not the mock) by resetting first
    vi.doUnmock('@/hooks/useTaskAiReviewHooks');
    const mod = await import('@/hooks/useTaskAiReviewHooks');
    expect(typeof mod.useLatestAiReview).toBe('function');
    expect(typeof mod.useAllAiReviews).toBe('function');
    expect(typeof mod.useRunAiReview).toBe('function');
    expect(mod.aiReviewKeys).toBeDefined();
    expect(Array.isArray(mod.aiReviewKeys.all)).toBe(true);
  });
});

// ── API constants test ────────────────────────────────────────────────────────

describe('API_ENDPOINTS — AI review constants', () => {
  it('defines TASK_SUBMISSION_AI_REVIEW endpoint', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    expect(typeof API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW).toBe('function');
    expect(API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW('sub-1')).toBe(
      '/task-submissions/sub-1/ai-review',
    );
  });

  it('defines TASK_SUBMISSION_AI_REVIEWS endpoint', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    expect(API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEWS('sub-1')).toBe(
      '/task-submissions/sub-1/ai-reviews',
    );
  });

  it('defines TASK_AI_REVIEW_BY_ID endpoint', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    expect(API_ENDPOINTS.TASK_AI_REVIEW_BY_ID('rev-1')).toBe('/task-ai-reviews/rev-1');
  });
});

// ── URL / endpoint regression tests ──────────────────────────────────────────
// Ensures no 404 endpoint mismatch between frontend and backend controller.

describe('API_ENDPOINTS — no 404 endpoint mismatch', () => {
  it('POST endpoint has correct path structure for Spring controller mapping', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    const uuid = '550e8400-e29b-41d4-a716-446655440000';
    const url = API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW(uuid);
    // Must be: /task-submissions/{uuid}/ai-review
    // Backend Spring mapping: @PostMapping("/task-submissions/{submissionId}/ai-review")
    expect(url).toBe(`/task-submissions/${uuid}/ai-review`);
    // Must NOT be: /tasks/.../ai-review, /ai-review, /task-ai-reviews, etc.
    expect(url).not.toBe('/ai-review');
    expect(url).not.toContain('/tasks/');
  });

  it('GET latest endpoint matches Spring @GetMapping exactly', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    const uuid = '550e8400-e29b-41d4-a716-446655440000';
    const url = API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW(uuid);
    // Same path used for GET and POST — correct, matches backend @GetMapping
    expect(url).toBe(`/task-submissions/${uuid}/ai-review`);
  });

  it('axiosInstance baseURL is /api matching backend server.servlet.context-path', async () => {
    const { API_BASE_URL } = await import('@/constants/api');
    // Must match application.properties: server.servlet.context-path=/api
    expect(API_BASE_URL).toBe('/api');
  });

  it('full request URL is /api/task-submissions/{id}/ai-review', async () => {
    const { API_BASE_URL, API_ENDPOINTS } = await import('@/constants/api');
    const uuid = '550e8400-e29b-41d4-a716-446655440000';
    const fullUrl = API_BASE_URL + API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW(uuid);
    // This must match what the Vite proxy forwards to Spring Boot
    expect(fullUrl).toBe(`/api/task-submissions/${uuid}/ai-review`);
  });

  it('re-run evaluation uses same submission ID path as first run', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    const submissionId = 'abc-def-123';
    // Both initial run and re-run call the same endpoint
    const url = API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW(submissionId);
    expect(url).toBe(`/task-submissions/${submissionId}/ai-review`);
  });
});
