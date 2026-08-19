/**
 * @fileoverview Phase 8A Analytics Dashboard — frontend tests.
 *
 * Covers:
 *   - Loading state (spinner shown)
 *   - Error state (alert + retry button)
 *   - KPI cards for privileged roles
 *   - KPI cards for EMPLOYEE role (restricted)
 *   - Attendance rate display
 *   - Task completion rate display
 *   - Chart rendering with data
 *   - Empty chart states
 *   - Filter bar rendering (privileged vs. employee)
 *   - Refresh button functionality
 *   - EMPLOYEE restrictions (no performance/department data)
 *   - AnalyticsKpiCard component unit tests
 *   - analyticsApi module unit tests
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';

// ── Module mocks ──────────────────────────────────────────────────────────────

vi.mock('@/hooks/useAnalytics', () => ({
  useAnalyticsSummary: vi.fn(),
  useAnalyticsAttendance: vi.fn(),
  useAnalyticsLeaves: vi.fn(),
  useAnalyticsTasks: vi.fn(),
  useAnalyticsPerformance: vi.fn(),
  useAnalyticsDepartments: vi.fn(),
  useRefreshAnalytics: vi.fn(),
  ANALYTICS_QUERY_KEYS: {},
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
  AuthContext: { Provider: ({ children }) => children },
}));

// Mock recharts so we don't need a DOM canvas environment
vi.mock('recharts', () => ({
  LineChart: ({ children }) => <div data-testid="line-chart">{children}</div>,
  BarChart: ({ children }) => <div data-testid="bar-chart">{children}</div>,
  PieChart: ({ children }) => <div data-testid="pie-chart">{children}</div>,
  Line: () => null,
  Bar: () => null,
  Pie: () => null,
  Cell: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
  ResponsiveContainer: ({ children }) => <div>{children}</div>,
}));

import AnalyticsDashboardPage from '@/pages/analytics/AnalyticsDashboardPage';
import AnalyticsKpiCard from '@/components/analytics/AnalyticsKpiCard';
import AnalyticsFiltersBar from '@/components/analytics/AnalyticsFiltersBar';

import {
  useAnalyticsSummary,
  useAnalyticsAttendance,
  useAnalyticsLeaves,
  useAnalyticsTasks,
  useAnalyticsPerformance,
  useAnalyticsDepartments,
  useRefreshAnalytics,
} from '@/hooks/useAnalytics';
import { useAuth } from '@/contexts/AuthContext';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const SUMMARY_DATA = {
  totalEmployees: 120,
  activeEmployees: 110,
  inactiveEmployees: 10,
  employeesOnLeave: 5,
  newEmployees: 3,
  attendanceRate: 0.87,
  presentCount: 2400,
  absentCount: 200,
  halfDayCount: 50,
  onLeaveCount: 150,
  totalLeaveRequests: 80,
  pendingLeaveRequests: 12,
  approvedLeaveRequests: 55,
  rejectedLeaveRequests: 13,
  totalTasks: 95,
  completedTasks: 60,
  pendingTasks: 25,
  overdueTasks: 10,
  taskCompletionRate: 0.63,
  avgAiScore: 78.5,
  completedAiEvaluations: 42,
  failedAiEvaluations: 3,
  attendanceTrend: [{ label: '2024-06-01', value: 0.87 }],
  aiScoreTrend: [{ label: '2024-06-01', value: 78.5 }],
};

const ATTENDANCE_DATA = {
  totalRecords: 2800,
  presentCount: 2400,
  absentCount: 200,
  halfDayCount: 50,
  workFromHomeCount: 100,
  onLeaveCount: 50,
  attendanceRate: 0.87,
  trend: [{ date: '2024-06-01', present: 95, absent: 5, total: 100, rate: 0.95 }],
};

const LEAVES_DATA = {
  totalRequests: 80,
  pendingCount: 12,
  approvedCount: 55,
  rejectedCount: 8,
  cancelledCount: 5,
  leaveUtilizationRate: 0.69,
  byType: [
    { leaveType: 'ANNUAL', count: 30 },
    { leaveType: 'SICK', count: 20 },
  ],
  trend: [{ month: '2024-06', total: 15, approved: 10 }],
};

const TASKS_DATA = {
  totalTasks: 95,
  completedTasks: 60,
  assignedTasks: 10,
  inProgressTasks: 12,
  submittedTasks: 3,
  overdueTasks: 7,
  draftTasks: 3,
  completionRate: 0.63,
  statusBreakdown: [
    { status: 'COMPLETED', count: 60 },
    { status: 'IN_PROGRESS', count: 12 },
    { status: 'OVERDUE', count: 7 },
  ],
};

const PERFORMANCE_DATA = {
  avgCompletionScore: 78.5,
  avgQualityScore: 74.2,
  completedEvaluations: 42,
  failedEvaluations: 3,
  pendingEvaluations: 1,
  scoreTrend: [{ label: '2024-06-01', avgScore: 82.3, evaluationCount: 8 }],
};

const DEPTS_DATA = {
  totalDepartments: 8,
  departments: [
    {
      departmentId: 'dept-1',
      departmentName: 'Engineering',
      departmentCode: 'ENG',
      headcount: 25,
      activeCount: 23,
      onLeaveCount: 2,
    },
    {
      departmentId: 'dept-2',
      departmentName: 'HR',
      departmentCode: 'HR',
      headcount: 10,
      activeCount: 9,
      onLeaveCount: 1,
    },
  ],
};

// ── Test helpers ──────────────────────────────────────────────────────────────

const theme = createTheme();

function renderPage(ui) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <HelmetProvider>
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={qc}>
          <MemoryRouter>{ui}</MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    </HelmetProvider>,
  );
}

function idleHook(data) {
  return { data, isLoading: false, isFetching: false, isError: false, error: null };
}

function loadingHook() {
  return { data: undefined, isLoading: true, isFetching: true, isError: false, error: null };
}

function errorHook(message = 'Network error') {
  return {
    data: undefined,
    isLoading: false,
    isFetching: false,
    isError: true,
    error: { message },
  };
}

function setupPrivilegedHooks(overrides = {}) {
  useAnalyticsSummary.mockReturnValue(idleHook(SUMMARY_DATA));
  useAnalyticsAttendance.mockReturnValue(idleHook(ATTENDANCE_DATA));
  useAnalyticsLeaves.mockReturnValue(idleHook(LEAVES_DATA));
  useAnalyticsTasks.mockReturnValue(idleHook(TASKS_DATA));
  useAnalyticsPerformance.mockReturnValue(idleHook(PERFORMANCE_DATA));
  useAnalyticsDepartments.mockReturnValue(idleHook(DEPTS_DATA));
  useRefreshAnalytics.mockReturnValue(vi.fn());
  useAuth.mockReturnValue({
    user: { userId: 'u1', roles: ['ROLE_ADMIN'] },
    hasAnyRole: (roles) => roles.includes('ROLE_ADMIN'),
    ...overrides,
  });
}

function setupEmployeeHooks(overrides = {}) {
  useAnalyticsSummary.mockReturnValue(idleHook(SUMMARY_DATA));
  useAnalyticsAttendance.mockReturnValue(idleHook(ATTENDANCE_DATA));
  useAnalyticsLeaves.mockReturnValue(idleHook(LEAVES_DATA));
  useAnalyticsTasks.mockReturnValue(idleHook(TASKS_DATA));
  useAnalyticsPerformance.mockReturnValue(idleHook(null));
  useAnalyticsDepartments.mockReturnValue(idleHook(null));
  useRefreshAnalytics.mockReturnValue(vi.fn());
  useAuth.mockReturnValue({
    user: { userId: 'u2', roles: ['ROLE_EMPLOYEE'] },
    hasAnyRole: (roles) =>
      roles.includes('ROLE_EMPLOYEE') &&
      !roles.some((r) => ['ROLE_ADMIN', 'ROLE_HR', 'ROLE_MANAGER'].includes(r)),
    ...overrides,
  });
}

// ── Tests ─────────────────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks();
});

// ── Loading state ──────────────────────────────────────────────────────────────

describe('AnalyticsDashboardPage — loading state', () => {
  it('shows a loading spinner while summary is loading', () => {
    useAnalyticsSummary.mockReturnValue(loadingHook());
    useAnalyticsAttendance.mockReturnValue(idleHook(null));
    useAnalyticsLeaves.mockReturnValue(idleHook(null));
    useAnalyticsTasks.mockReturnValue(idleHook(null));
    useAnalyticsPerformance.mockReturnValue(idleHook(null));
    useAnalyticsDepartments.mockReturnValue(idleHook(null));
    useRefreshAnalytics.mockReturnValue(vi.fn());
    useAuth.mockReturnValue({
      user: { userId: 'u1', roles: ['ROLE_ADMIN'] },
      hasAnyRole: (roles) => roles.includes('ROLE_ADMIN'),
    });

    renderPage(<AnalyticsDashboardPage />);

    expect(screen.getByLabelText('Loading analytics')).toBeInTheDocument();
  });
});

// ── Error state ────────────────────────────────────────────────────────────────

describe('AnalyticsDashboardPage — error state', () => {
  it('shows an error alert when summary fails and no cached data', () => {
    useAnalyticsSummary.mockReturnValue(errorHook('Service unavailable'));
    useAnalyticsAttendance.mockReturnValue(idleHook(null));
    useAnalyticsLeaves.mockReturnValue(idleHook(null));
    useAnalyticsTasks.mockReturnValue(idleHook(null));
    useAnalyticsPerformance.mockReturnValue(idleHook(null));
    useAnalyticsDepartments.mockReturnValue(idleHook(null));
    useRefreshAnalytics.mockReturnValue(vi.fn());
    useAuth.mockReturnValue({
      user: { userId: 'u1', roles: ['ROLE_ADMIN'] },
      hasAnyRole: (roles) => roles.includes('ROLE_ADMIN'),
    });

    renderPage(<AnalyticsDashboardPage />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('Service unavailable')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('calls refresh when Retry button is clicked', () => {
    const refresh = vi.fn();
    useAnalyticsSummary.mockReturnValue(errorHook('Network error'));
    useAnalyticsAttendance.mockReturnValue(idleHook(null));
    useAnalyticsLeaves.mockReturnValue(idleHook(null));
    useAnalyticsTasks.mockReturnValue(idleHook(null));
    useAnalyticsPerformance.mockReturnValue(idleHook(null));
    useAnalyticsDepartments.mockReturnValue(idleHook(null));
    useRefreshAnalytics.mockReturnValue(refresh);
    useAuth.mockReturnValue({
      user: { userId: 'u1', roles: ['ROLE_ADMIN'] },
      hasAnyRole: (roles) => roles.includes('ROLE_ADMIN'),
    });

    renderPage(<AnalyticsDashboardPage />);
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));

    expect(refresh).toHaveBeenCalled();
  });
});

// ── ADMIN / HR / MANAGER — full dashboard ─────────────────────────────────────

describe('AnalyticsDashboardPage — privileged role (ADMIN)', () => {
  beforeEach(() => setupPrivilegedHooks());

  it('renders the "HR Analytics" heading', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByRole('heading', { name: /HR Analytics/i })).toBeInTheDocument();
  });

  it('renders "Organisation-wide insights" subtitle', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByText(/Organisation-wide insights/i)).toBeInTheDocument();
  });

  it('renders Total Employees KPI card', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByLabelText('Total Employees KPI card')).toBeInTheDocument();
    expect(screen.getByText('120')).toBeInTheDocument();
  });

  it('renders Attendance Rate KPI card with formatted percentage', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByLabelText('Attendance Rate KPI card')).toBeInTheDocument();
    // 0.87 → "87.0%"
    expect(screen.getByText('87.0%')).toBeInTheDocument();
  });

  it('renders Task Completion KPI card', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByLabelText('Task Completion KPI card')).toBeInTheDocument();
    expect(screen.getByText('63.0%')).toBeInTheDocument();
  });

  it('renders AI Score KPI card', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByLabelText('Avg AI Score KPI card')).toBeInTheDocument();
    expect(screen.getByText('78.5')).toBeInTheDocument();
  });

  it('renders Leave Requests KPI card', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByLabelText('Leave Requests KPI card')).toBeInTheDocument();
    expect(screen.getByText('80')).toBeInTheDocument();
  });

  it('renders the Attendance Trend chart section', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByText('Attendance Trend')).toBeInTheDocument();
  });

  it('renders the Leave Distribution chart section', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByText('Leave Distribution')).toBeInTheDocument();
  });

  it('renders the Task Status chart section', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByText('Task Status')).toBeInTheDocument();
  });

  it('renders the Department Headcount chart section', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByText('Department Headcount')).toBeInTheDocument();
  });

  it('renders the AI Performance Trend chart section', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByText('AI Performance Trend')).toBeInTheDocument();
  });

  it('renders the filter bar', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByRole('region', { name: /Analytics filters/i })).toBeInTheDocument();
  });
});

// ── EMPLOYEE — restricted dashboard ───────────────────────────────────────────

describe('AnalyticsDashboardPage — EMPLOYEE role', () => {
  beforeEach(() => setupEmployeeHooks());

  it('renders "Your personal analytics" subtitle', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByText(/Your personal analytics/i)).toBeInTheDocument();
  });

  it('does NOT render Total Employees KPI card', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.queryByLabelText('Total Employees KPI card')).not.toBeInTheDocument();
  });

  it('does NOT render AI Score KPI card', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.queryByLabelText('Avg AI Score KPI card')).not.toBeInTheDocument();
  });

  it('does NOT render AI Performance Trend section', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.queryByText('AI Performance Trend')).not.toBeInTheDocument();
  });

  it('does NOT render Department Headcount section', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.queryByText('Department Headcount')).not.toBeInTheDocument();
  });

  it('still renders Attendance Rate KPI card', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByLabelText('Attendance Rate KPI card')).toBeInTheDocument();
  });

  it('still renders Task Completion KPI card', () => {
    renderPage(<AnalyticsDashboardPage />);
    expect(screen.getByLabelText('Task Completion KPI card')).toBeInTheDocument();
  });
});

// ── Empty states ───────────────────────────────────────────────────────────────

describe('AnalyticsDashboardPage — empty dataset states', () => {
  it('shows empty state for attendance chart when no trend data', () => {
    setupPrivilegedHooks();
    useAnalyticsAttendance.mockReturnValue(
      idleHook({
        ...ATTENDANCE_DATA,
        trend: [],
      }),
    );

    renderPage(<AnalyticsDashboardPage />);

    expect(screen.getByText('No attendance records for this period')).toBeInTheDocument();
  });

  it('shows empty state for leave distribution chart when no type data', () => {
    setupPrivilegedHooks();
    useAnalyticsLeaves.mockReturnValue(
      idleHook({
        ...LEAVES_DATA,
        byType: [],
      }),
    );

    renderPage(<AnalyticsDashboardPage />);

    expect(screen.getByText('No leave requests in this period')).toBeInTheDocument();
  });

  it('shows empty state for task chart when no tasks', () => {
    setupPrivilegedHooks();
    useAnalyticsTasks.mockReturnValue(
      idleHook({
        ...TASKS_DATA,
        statusBreakdown: [],
      }),
    );

    renderPage(<AnalyticsDashboardPage />);

    expect(screen.getByText('No tasks found')).toBeInTheDocument();
  });

  it('shows empty state for AI performance when no trend data', () => {
    setupPrivilegedHooks();
    useAnalyticsPerformance.mockReturnValue(
      idleHook({
        ...PERFORMANCE_DATA,
        scoreTrend: [],
      }),
    );

    renderPage(<AnalyticsDashboardPage />);

    expect(screen.getByText('No AI evaluations in this period')).toBeInTheDocument();
  });
});

// ── Refresh ────────────────────────────────────────────────────────────────────

describe('AnalyticsDashboardPage — refresh', () => {
  it('calls refreshAll when Refresh button is clicked in filter bar', () => {
    const refresh = vi.fn();
    setupPrivilegedHooks();
    useRefreshAnalytics.mockReturnValue(refresh);

    renderPage(<AnalyticsDashboardPage />);

    const refreshButtons = screen.getAllByRole('button', { name: /refresh/i });
    expect(refreshButtons.length).toBeGreaterThan(0);
    fireEvent.click(refreshButtons[0]);
    expect(refresh).toHaveBeenCalled();
  });
});

// ── AnalyticsKpiCard unit tests ───────────────────────────────────────────────

describe('AnalyticsKpiCard', () => {
  function renderCard(props) {
    return render(
      <ThemeProvider theme={theme}>
        <AnalyticsKpiCard {...props} />
      </ThemeProvider>,
    );
  }

  it('renders the label and value', () => {
    renderCard({ icon: <span />, label: 'Total Employees', value: '120' });
    expect(screen.getByText('Total Employees')).toBeInTheDocument();
    expect(screen.getByText('120')).toBeInTheDocument();
  });

  it('renders a loading skeleton when loading=true', () => {
    renderCard({ icon: <span />, label: 'Test', value: '0', loading: true });
    // MUI Skeleton renders as a div; there should be no value text
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('renders sub-value text', () => {
    renderCard({
      icon: <span />,
      label: 'Test',
      value: '90',
      subValue: '5 on leave',
    });
    expect(screen.getByText('5 on leave')).toBeInTheDocument();
  });

  it('renders the card as a region with accessible label', () => {
    renderCard({ icon: <span />, label: 'Attendance Rate', value: '87%' });
    expect(screen.getByRole('region', { name: /Attendance Rate KPI card/i })).toBeInTheDocument();
  });

  it('renders trend label when provided', () => {
    renderCard({
      icon: <span />,
      label: 'Test',
      value: '5',
      trend: 2,
      trendLabel: '+2 vs last week',
    });
    expect(screen.getByText('+2 vs last week')).toBeInTheDocument();
  });
});

// ── AnalyticsFiltersBar unit tests ────────────────────────────────────────────

describe('AnalyticsFiltersBar', () => {
  function renderBar(props) {
    return render(
      <ThemeProvider theme={theme}>
        <MemoryRouter>
          <AnalyticsFiltersBar
            filters={{ from: '2024-06-01', to: '2024-06-30' }}
            onFiltersChange={vi.fn()}
            onRefresh={vi.fn()}
            {...props}
          />
        </MemoryRouter>
      </ThemeProvider>,
    );
  }

  it('renders the Filters heading', () => {
    renderBar();
    expect(screen.getByText('Filters')).toBeInTheDocument();
  });

  it('renders date range inputs', () => {
    renderBar();
    expect(screen.getByLabelText('Start date')).toBeInTheDocument();
    expect(screen.getByLabelText('End date')).toBeInTheDocument();
  });

  it('does NOT render department filter when isPrivileged is false', () => {
    renderBar({ isPrivileged: false, departments: [{ id: 'd1', name: 'Eng' }] });
    expect(screen.queryByLabelText('Department filter')).not.toBeInTheDocument();
  });

  it('renders department filter when isPrivileged is true and departments provided', () => {
    renderBar({ isPrivileged: true, departments: [{ id: 'd1', name: 'Engineering' }] });
    expect(screen.getByLabelText('Department filter')).toBeInTheDocument();
  });

  it('calls onFiltersChange when a preset chip is clicked', () => {
    const onChange = vi.fn();
    renderBar({ onFiltersChange: onChange });
    fireEvent.click(screen.getByText('7 days'));
    expect(onChange).toHaveBeenCalled();
  });

  it('calls onRefresh when Refresh button is clicked', () => {
    const onRefresh = vi.fn();
    renderBar({ onRefresh });
    fireEvent.click(screen.getByRole('button', { name: /refresh/i }));
    expect(onRefresh).toHaveBeenCalled();
  });
});

// ── analyticsApi unit tests ───────────────────────────────────────────────────

describe('analyticsApi', () => {
  it('exports getAnalyticsSummary', async () => {
    const { getAnalyticsSummary } = await import('@/services/analyticsApi');
    expect(typeof getAnalyticsSummary).toBe('function');
  });

  it('exports getAnalyticsAttendance', async () => {
    const { getAnalyticsAttendance } = await import('@/services/analyticsApi');
    expect(typeof getAnalyticsAttendance).toBe('function');
  });

  it('exports getAnalyticsLeaves', async () => {
    const { getAnalyticsLeaves } = await import('@/services/analyticsApi');
    expect(typeof getAnalyticsLeaves).toBe('function');
  });

  it('exports getAnalyticsTasks', async () => {
    const { getAnalyticsTasks } = await import('@/services/analyticsApi');
    expect(typeof getAnalyticsTasks).toBe('function');
  });

  it('exports getAnalyticsPerformance', async () => {
    const { getAnalyticsPerformance } = await import('@/services/analyticsApi');
    expect(typeof getAnalyticsPerformance).toBe('function');
  });

  it('exports getAnalyticsDepartments', async () => {
    const { getAnalyticsDepartments } = await import('@/services/analyticsApi');
    expect(typeof getAnalyticsDepartments).toBe('function');
  });
});

// ── useAnalytics hooks mock-shape tests ──────────────────────────────────────

describe('useAnalytics hooks', () => {
  it('all mock hooks are callable vi.fn() functions', () => {
    // The module is mocked at the top of this file.
    // Verify that each exported hook is a function (the vi.fn() mock).
    expect(typeof useAnalyticsSummary).toBe('function');
    expect(typeof useAnalyticsAttendance).toBe('function');
    expect(typeof useAnalyticsLeaves).toBe('function');
    expect(typeof useAnalyticsTasks).toBe('function');
    expect(typeof useAnalyticsPerformance).toBe('function');
    expect(typeof useAnalyticsDepartments).toBe('function');
    expect(typeof useRefreshAnalytics).toBe('function');
  });
});
