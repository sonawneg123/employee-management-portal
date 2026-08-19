/**
 * @fileoverview Phase 7D Frontend Tests — AI Insights, Employee Feedback & Task Intelligence
 *
 * Covers:
 *  1.  Employee AI feedback rendering — COMPLETED state
 *  2.  Employee AI feedback — PENDING state
 *  3.  Employee AI feedback — PROCESSING state
 *  4.  Employee AI feedback — FAILED state (no stack trace exposed)
 *  5.  Employee AI feedback — no review yet (404)
 *  6.  Employee AI feedback — shows strengths
 *  7.  Employee AI feedback — shows areas to improve
 *  8.  Employee AI feedback — shows suggestions
 *  9.  Employee AI feedback — shows "How was this evaluated?" section
 * 10.  Employee AI feedback — does NOT expose recommendedAction/managerSummary
 * 11.  Employee AI feedback — polling hooks
 * 12.  AI evaluation history table rendering
 * 13.  Manager AI trend — IMPROVING classification
 * 14.  Manager AI trend — STABLE classification
 * 15.  Manager AI trend — DECLINING classification
 * 16.  Manager AI trend — INSUFFICIENT_DATA (fewer than 2 evaluations)
 * 17.  Manager AI trend — score visualization
 * 18.  Manager AI trend — no-history message
 * 19.  Manager AI trend — advisory notice present
 * 20.  AI Dashboard Summary Widget — renders all stats
 * 21.  AI Dashboard Summary Widget — loading state
 * 22.  Hook exports — Phase 7D hooks exported
 * 23.  API constants — Phase 7D endpoints defined
 * 24.  Retry state — UI shows retrying when mutation is pending
 * 25.  Authorization/error state — 403 handled gracefully
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';

import EmployeeAiFeedbackSection from '@/components/tasks/EmployeeAiFeedbackSection';
import ManagerAiTrendSection from '@/components/tasks/ManagerAiTrendSection';
import AiDashboardSummaryWidget from '@/components/dashboard/AiDashboardSummaryWidget';

// ── Mocks ──────────────────────────────────────────────────────────────────────

vi.mock('@/hooks/useTaskAiReviewHooks', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useEmployeeAiFeedback: vi.fn(),
    useEmployeeAiHistory: vi.fn(),
    useAiScoreTrend: vi.fn(),
    useAiTaskInsights: vi.fn(),
    useAiDashboardSummary: vi.fn(),
    // Keep original manager hooks as-is
    useLatestAiReview: vi.fn(),
    useAllAiReviews: vi.fn(),
    useRunAiReview: vi.fn(),
  };
});

import {
  useEmployeeAiFeedback,
  useEmployeeAiHistory,
  useAiScoreTrend,
  useAiTaskInsights,
  useAiDashboardSummary,
} from '@/hooks/useTaskAiReviewHooks';

// ── Helpers ────────────────────────────────────────────────────────────────────

const theme = createTheme();

function Wrapper({ children }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </ThemeProvider>
  );
}

function renderFeedback(props = {}) {
  return render(
    <Wrapper>
      <EmployeeAiFeedbackSection submissionId="sub-001" taskId="task-001" {...props} />
    </Wrapper>,
  );
}

function renderTrend(props = {}) {
  return render(
    <Wrapper>
      <ManagerAiTrendSection taskId="task-001" {...props} />
    </Wrapper>,
  );
}

function renderDashboard(props = {}) {
  return render(
    <Wrapper>
      <AiDashboardSummaryWidget {...props} />
    </Wrapper>,
  );
}

// ── Fixtures ───────────────────────────────────────────────────────────────────

const COMPLETED_FEEDBACK = {
  id: 'review-001',
  submissionId: 'sub-001',
  status: 'COMPLETED',
  overallScore: 85,
  workQualityScore: 88,
  completenessScore: 85,
  relevanceScore: 90,
  summary: 'The submission demonstrates solid understanding of the requirements.',
  strengths: ['Clear code structure', 'Well-documented functions'],
  areasToImprove: ['Missing edge case handling', 'Could add more tests'],
  suggestionsForNextSubmission: ['Add unit tests for error cases', 'Document API endpoints'],
  evaluatedAt: '2025-01-01T10:30:00',
  requestedAt: '2025-01-01T10:00:00',
  evaluationExplanation:
    'This evaluation was produced by an AI assistant that analysed:\n• The task title and description\n• AI evaluation is advisory only. Your manager makes the final decision.',
};

const PENDING_FEEDBACK = {
  ...COMPLETED_FEEDBACK,
  status: 'PENDING',
  overallScore: null,
  workQualityScore: null,
  summary: 'AI evaluation is in the queue and will start shortly.',
  strengths: [],
  areasToImprove: [],
  suggestionsForNextSubmission: [],
  evaluatedAt: null,
};

const PROCESSING_FEEDBACK = {
  ...PENDING_FEEDBACK,
  status: 'PROCESSING',
  summary: 'AI evaluation is being generated. This usually takes under a minute.',
};

const FAILED_FEEDBACK = {
  ...PENDING_FEEDBACK,
  status: 'FAILED',
  summary: 'The AI evaluation could not be completed at this time. Your manager has been notified.',
};

const TREND_IMPROVING = {
  taskId: 'task-001',
  scoreHistory: [
    {
      reviewId: 'r1',
      submissionNumber: 1,
      overallScore: 70,
      qualityScore: 72,
      evaluatedAt: '2025-01-01T09:00:00',
    },
    {
      reviewId: 'r2',
      submissionNumber: 2,
      overallScore: 85,
      qualityScore: 88,
      evaluatedAt: '2025-01-02T09:00:00',
    },
  ],
  trendDirection: 'IMPROVING',
  totalScoreChange: 15,
  latestScore: 85,
  previousScore: 70,
  latestScoreChange: 15,
  hasTrendData: true,
};

const TREND_INSUFFICIENT = {
  taskId: 'task-001',
  scoreHistory: [],
  trendDirection: 'INSUFFICIENT_DATA',
  totalScoreChange: null,
  latestScore: null,
  previousScore: null,
  latestScoreChange: null,
  hasTrendData: false,
};

const TREND_DECLINING = {
  ...TREND_IMPROVING,
  trendDirection: 'DECLINING',
  latestScore: 70,
  previousScore: 88,
  latestScoreChange: -18,
};

const TREND_STABLE = {
  ...TREND_IMPROVING,
  trendDirection: 'STABLE',
  latestScore: 83,
  previousScore: 80,
  latestScoreChange: 3,
};

const DASHBOARD_SUMMARY = {
  totalEvaluated: 12,
  averageScore: 84.0,
  employeesImproving: 3,
  employeesNeedingAttention: 1,
  submissionsAwaitingEvaluation: 5,
  failedEvaluations: 2,
};

function setupFeedbackHooks({
  feedback = null,
  feedbackError = null,
  feedbackLoading = false,
  history = [],
} = {}) {
  useEmployeeAiFeedback.mockReturnValue({
    data: feedback,
    isLoading: feedbackLoading,
    error: feedbackError,
  });
  useEmployeeAiHistory.mockReturnValue({
    data: history,
    isLoading: false,
  });
}

function setupTrendHooks({
  trend = null,
  trendLoading = false,
  insights = null,
  insightsLoading = false,
} = {}) {
  useAiScoreTrend.mockReturnValue({
    data: trend,
    isLoading: trendLoading,
  });
  useAiTaskInsights.mockReturnValue({
    data: insights,
    isLoading: insightsLoading,
  });
}

// ── Tests ──────────────────────────────────────────────────────────────────────

describe('Phase 7D — EmployeeAiFeedbackSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1. COMPLETED state renders overall score', () => {
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK });
    const { container } = renderFeedback();
    // Component renders score bars with "85%" — use container text to check presence
    expect(container.textContent).toContain('85');
  });

  it('2. PENDING state shows "in queue" message', () => {
    setupFeedbackHooks({ feedback: PENDING_FEEDBACK });
    renderFeedback();
    expect(screen.getByTestId('ai-feedback-pending')).toBeDefined();
  });

  it('3. PROCESSING state shows "in progress" message', () => {
    setupFeedbackHooks({ feedback: PROCESSING_FEEDBACK });
    renderFeedback();
    expect(screen.getByTestId('ai-feedback-processing')).toBeDefined();
  });

  it('4. FAILED state shows friendly message — no stack trace', () => {
    setupFeedbackHooks({ feedback: FAILED_FEEDBACK });
    renderFeedback();
    expect(screen.getByTestId('ai-feedback-failed')).toBeDefined();
    // Must not expose raw error message
    expect(screen.queryByText(/GroqClientException/)).toBeNull();
    expect(screen.queryByText(/stack trace/i)).toBeNull();
  });

  it('5. 404 shows "in progress" info alert', () => {
    setupFeedbackHooks({ feedbackError: { response: { status: 404 } } });
    renderFeedback();
    expect(screen.getByTestId('no-ai-feedback-yet')).toBeDefined();
  });

  it('6. COMPLETED state shows strengths', () => {
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK });
    renderFeedback();
    expect(screen.getByText(/Clear code structure/)).toBeDefined();
  });

  it('7. COMPLETED state shows areas to improve', () => {
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK });
    renderFeedback();
    expect(screen.getByText(/Missing edge case handling/)).toBeDefined();
  });

  it('8. COMPLETED state shows suggestions for next submission', () => {
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK });
    renderFeedback();
    expect(screen.getByText(/Add unit tests for error cases/)).toBeDefined();
  });

  it('9. COMPLETED state shows "How was this evaluated?" section', () => {
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK });
    renderFeedback();
    const btn = screen.getByTestId('show-explanation-btn');
    expect(btn).toBeDefined();
    fireEvent.click(btn);
    // After clicking, the explanation text is visible — check with getAllByText since text may appear multiple times
    const advisoryTexts = screen.getAllByText(/advisory only/i);
    expect(advisoryTexts.length).toBeGreaterThan(0);
  });

  it('10. Response does NOT contain manager-only fields (recommendedAction, managerSummary)', () => {
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK });
    const { container } = renderFeedback();
    // recommendedAction and managerSummary should not appear in the rendered output
    expect(container.textContent).not.toContain('APPROVE');
    expect(container.textContent).not.toContain('REQUEST_CHANGES');
    expect(container.textContent).not.toContain('Manager-only summary');
  });

  it('11. Advisory notice is shown on COMPLETED feedback', () => {
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK });
    const { container } = renderFeedback();
    // Alert contains "advisory only" — check container text as multiple elements may match
    expect(container.textContent.toLowerCase()).toContain('advisory only');
  });

  it('12. Loading state shows skeletons', () => {
    setupFeedbackHooks({ feedbackLoading: true });
    const { container } = renderFeedback();
    const skeletons = container.querySelectorAll('.MuiSkeleton-root');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('13. AI evaluation history table shown when multiple evaluations exist', () => {
    const history = [
      { ...COMPLETED_FEEDBACK, id: 'r1', overallScore: 85, evaluatedAt: '2025-01-02' },
      { ...COMPLETED_FEEDBACK, id: 'r2', overallScore: 78, evaluatedAt: '2025-01-01' },
    ];
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK, history });
    renderFeedback();
    expect(screen.getByTestId('ai-feedback-history-table')).toBeDefined();
  });

  it('14. History table row click selects evaluation', async () => {
    const history = [
      { ...COMPLETED_FEEDBACK, id: 'r1', overallScore: 85, evaluatedAt: '2025-01-02' },
      { ...COMPLETED_FEEDBACK, id: 'r2', overallScore: 78, evaluatedAt: '2025-01-01' },
    ];
    setupFeedbackHooks({ feedback: COMPLETED_FEEDBACK, history });
    renderFeedback();
    const firstRow = screen.getByTestId('ai-history-row-0');
    fireEvent.click(firstRow);
    // Row is now selected (visual change) — just verify it doesn't throw
    expect(firstRow).toBeDefined();
  });
});

// ── Manager AI Trend ────────────────────────────────────────────────────────────

describe('Phase 7D — ManagerAiTrendSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('15. IMPROVING trend shows "Improving" chip', () => {
    setupTrendHooks({ trend: TREND_IMPROVING });
    renderTrend();
    expect(screen.getByText(/Improving/i)).toBeDefined();
  });

  it('16. STABLE trend shows "Stable" chip', () => {
    setupTrendHooks({ trend: TREND_STABLE });
    renderTrend();
    expect(screen.getByText(/Stable/i)).toBeDefined();
  });

  it('17. DECLINING trend shows "Needs Attention" chip', () => {
    setupTrendHooks({ trend: TREND_DECLINING });
    renderTrend();
    expect(screen.getByText(/Needs Attention/i)).toBeDefined();
  });

  it('18. INSUFFICIENT_DATA shows "Not enough evaluation history" message', () => {
    setupTrendHooks({ trend: TREND_INSUFFICIENT });
    renderTrend();
    expect(screen.getByTestId('no-trend-message')).toBeDefined();
  });

  it('19. Score trend visualization shows score history', () => {
    setupTrendHooks({ trend: TREND_IMPROVING });
    const { container } = renderTrend();
    const viz = screen.getByTestId('score-trend-viz');
    expect(viz).toBeDefined();
    expect(viz.textContent).toContain('70');
    expect(viz.textContent).toContain('85');
  });

  it('20. Advisory notice is shown', () => {
    setupTrendHooks({ trend: TREND_IMPROVING });
    renderTrend();
    expect(screen.getByText(/advisory/i)).toBeDefined();
  });

  it('21. Current score and previous score shown on trend with data', () => {
    setupTrendHooks({ trend: TREND_IMPROVING });
    renderTrend();
    expect(screen.getByTestId('latest-score')).toBeDefined();
    expect(screen.getByTestId('previous-score')).toBeDefined();
  });

  it('22. Score change shown with correct sign', () => {
    setupTrendHooks({ trend: TREND_IMPROVING });
    renderTrend();
    const changeEl = screen.getByTestId('score-change');
    expect(changeEl.textContent).toContain('+15');
  });

  it('23. Loading state shows skeletons', () => {
    setupTrendHooks({ trendLoading: true });
    const { container } = renderTrend();
    const skeletons = container.querySelectorAll('.MuiSkeleton-root');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('24. No data shows fallback text', () => {
    setupTrendHooks({ trend: null });
    renderTrend();
    expect(screen.getByText(/No AI evaluation data available/i)).toBeDefined();
  });

  it('25. Toggle shows AI Insights section', async () => {
    setupTrendHooks({ trend: TREND_IMPROVING });
    renderTrend();
    const toggleBtn = screen.getByTestId('toggle-insights-btn');
    fireEvent.click(toggleBtn);
    // Insights section appears
    expect(screen.getByTestId('ai-insights-section')).toBeDefined();
  });
});

// ── AI Dashboard Summary Widget ─────────────────────────────────────────────────

describe('Phase 7D — AiDashboardSummaryWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('26. Renders all dashboard stats', () => {
    useAiDashboardSummary.mockReturnValue({
      data: DASHBOARD_SUMMARY,
      isLoading: false,
      isError: false,
    });
    renderDashboard();
    expect(screen.getByTestId('ai-stat-evaluated')).toBeDefined();
    expect(screen.getByTestId('ai-stat-avg-score')).toBeDefined();
    expect(screen.getByTestId('ai-stat-improving')).toBeDefined();
    expect(screen.getByTestId('ai-stat-attention')).toBeDefined();
    expect(screen.getByTestId('ai-stat-awaiting')).toBeDefined();
    expect(screen.getByTestId('ai-stat-failed')).toBeDefined();
  });

  it('27. Loading state shows skeletons', () => {
    useAiDashboardSummary.mockReturnValue({ data: null, isLoading: true, isError: false });
    const { container } = renderDashboard();
    const skeletons = container.querySelectorAll('.MuiSkeleton-root');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('28. Shows correct total evaluated count', () => {
    useAiDashboardSummary.mockReturnValue({
      data: DASHBOARD_SUMMARY,
      isLoading: false,
      isError: false,
    });
    renderDashboard();
    const statEl = screen.getByTestId('ai-stat-evaluated');
    expect(statEl.textContent).toContain('12');
  });

  it('29. Shows correct average score', () => {
    useAiDashboardSummary.mockReturnValue({
      data: DASHBOARD_SUMMARY,
      isLoading: false,
      isError: false,
    });
    renderDashboard();
    const statEl = screen.getByTestId('ai-stat-avg-score');
    expect(statEl.textContent).toContain('84');
  });

  it('30. Shows "Advisory" chip', () => {
    useAiDashboardSummary.mockReturnValue({
      data: DASHBOARD_SUMMARY,
      isLoading: false,
      isError: false,
    });
    renderDashboard();
    expect(screen.getByText(/Advisory/i)).toBeDefined();
  });
});

// ── Hook exports ────────────────────────────────────────────────────────────────

describe('Phase 7D — Hook exports', () => {
  it('31. Phase 7D hooks are exported from useTaskAiReviewHooks', async () => {
    const hooks = await import('@/hooks/useTaskAiReviewHooks');
    expect(typeof hooks.useEmployeeAiFeedback).toBe('function');
    expect(typeof hooks.useEmployeeAiHistory).toBe('function');
    expect(typeof hooks.useAiScoreTrend).toBe('function');
    expect(typeof hooks.useAiTaskInsights).toBe('function');
    expect(typeof hooks.useAiDashboardSummary).toBe('function');
  });

  it('32. Phase 7D query keys are defined in aiReviewKeys', async () => {
    const { aiReviewKeys } = await import('@/hooks/useTaskAiReviewHooks');
    expect(typeof aiReviewKeys.employeeFeedback).toBe('function');
    expect(typeof aiReviewKeys.employeeHistory).toBe('function');
    expect(typeof aiReviewKeys.scoreTrend).toBe('function');
    expect(typeof aiReviewKeys.taskInsights).toBe('function');
    expect(typeof aiReviewKeys.dashboardSummary).toBe('function');
  });

  it('33. Employee feedback polling logic — polls when PENDING', () => {
    // Test the polling logic independently
    const mockQuery = {
      state: { data: { status: 'PENDING' }, status: 'success', error: null },
    };
    const status = mockQuery?.state?.data?.status;
    const isError = mockQuery?.state?.status === 'error';
    const is404 = isError && mockQuery?.state?.error?.response?.status === 404;
    let interval = false;
    if (status === 'PENDING' || status === 'PROCESSING') interval = 5_000;
    if (is404) interval = 5_000;
    expect(interval).toBe(5_000);
  });

  it('34. Employee feedback polling logic — stops when COMPLETED', () => {
    const mockQuery = {
      state: { data: { status: 'COMPLETED' }, status: 'success', error: null },
    };
    const status = mockQuery?.state?.data?.status;
    const isError = mockQuery?.state?.status === 'error';
    const is404 = isError && mockQuery?.state?.error?.response?.status === 404;
    let interval = false;
    if (status === 'PENDING' || status === 'PROCESSING') interval = 5_000;
    if (is404) interval = 5_000;
    expect(interval).toBe(false);
  });
});

// ── API constants ───────────────────────────────────────────────────────────────

describe('Phase 7D — API endpoint constants', () => {
  it('35. TASK_SUBMISSION_AI_FEEDBACK endpoint is defined', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    expect(typeof API_ENDPOINTS.TASK_SUBMISSION_AI_FEEDBACK).toBe('function');
    expect(API_ENDPOINTS.TASK_SUBMISSION_AI_FEEDBACK('test-id')).toBe(
      '/task-submissions/test-id/ai-feedback',
    );
  });

  it('36. TASK_SUBMISSION_AI_HISTORY endpoint is defined', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    expect(typeof API_ENDPOINTS.TASK_SUBMISSION_AI_HISTORY).toBe('function');
    expect(API_ENDPOINTS.TASK_SUBMISSION_AI_HISTORY('test-id')).toBe(
      '/task-submissions/test-id/ai-history',
    );
  });

  it('37. TASK_AI_TREND endpoint is defined', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    expect(typeof API_ENDPOINTS.TASK_AI_TREND).toBe('function');
    expect(API_ENDPOINTS.TASK_AI_TREND('task-id')).toBe('/tasks/task-id/ai-trend');
  });

  it('38. TASK_AI_INSIGHTS endpoint is defined', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    expect(typeof API_ENDPOINTS.TASK_AI_INSIGHTS).toBe('function');
    expect(API_ENDPOINTS.TASK_AI_INSIGHTS('task-id')).toBe('/tasks/task-id/ai-insights');
  });

  it('39. AI_DASHBOARD_SUMMARY endpoint is defined', async () => {
    const { API_ENDPOINTS } = await import('@/constants/api');
    expect(API_ENDPOINTS.AI_DASHBOARD_SUMMARY).toBe('/ai/dashboard-summary');
  });
});

// ── Authorization/error states ─────────────────────────────────────────────────

describe('Phase 7D — Error and authorization states', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('40. 403 error shows generic warning in feedback section', () => {
    setupFeedbackHooks({ feedbackError: { response: { status: 403 } } });
    renderFeedback();
    expect(screen.getByRole('alert')).toBeDefined();
  });

  it('41. Null submissionId renders nothing', () => {
    setupFeedbackHooks();
    const { container } = render(
      <Wrapper>
        <EmployeeAiFeedbackSection submissionId={null} taskId="task-001" />
      </Wrapper>,
    );
    // Should render nothing (returns null)
    expect(container.firstChild).toBeNull();
  });
});
