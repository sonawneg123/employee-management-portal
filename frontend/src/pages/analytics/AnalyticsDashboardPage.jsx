/**
 * @fileoverview AnalyticsDashboardPage — Phase 8A HR Analytics Dashboard.
 *
 * Displays KPI tiles, attendance trend, leave distribution, task status,
 * department headcount, and AI performance trend.
 *
 * RBAC:
 * - ADMIN / HR / MANAGER: full org-wide analytics with department filter.
 * - EMPLOYEE: scoped to their own data only; no org-wide stats visible.
 */

import React, { useCallback, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Grid,
  Stack,
  Typography,
  useTheme,
} from '@mui/material';
import PeopleRoundedIcon from '@mui/icons-material/PeopleRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import EventNoteRoundedIcon from '@mui/icons-material/EventNoteRounded';
import TaskAltRoundedIcon from '@mui/icons-material/TaskAltRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';
import {
  useAnalyticsSummary,
  useAnalyticsAttendance,
  useAnalyticsLeaves,
  useAnalyticsTasks,
  useAnalyticsPerformance,
  useAnalyticsDepartments,
  useRefreshAnalytics,
} from '@/hooks/useAnalytics';
import AnalyticsFiltersBar from '@/components/analytics/AnalyticsFiltersBar';
import AnalyticsKpiCard from '@/components/analytics/AnalyticsKpiCard';
import ChartCard from '@/components/dashboard/ChartCard';

// ── Colour palettes ───────────────────────────────────────────────────────────

const CHART_COLORS = {
  primary: '#1A2342',
  secondary: '#3B82F6',
  success: '#10B981',
  warning: '#F59E0B',
  error: '#EF4444',
  info: '#6366F1',
  gold: '#F5C518',
};

const LEAVE_TYPE_COLORS = [
  '#3B82F6',
  '#10B981',
  '#F59E0B',
  '#EF4444',
  '#6366F1',
  '#EC4899',
  '#14B8A6',
  '#8B5CF6',
];
const TASK_STATUS_COLORS = {
  COMPLETED: '#10B981',
  IN_PROGRESS: '#3B82F6',
  ASSIGNED: '#6366F1',
  SUBMITTED: '#F59E0B',
  OVERDUE: '#EF4444',
  DRAFT: '#9CA3AF',
};

// ── Helper ────────────────────────────────────────────────────────────────────

function toDateInput(d) {
  return d.toISOString().slice(0, 10);
}

function defaultFilters() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return { from: toDateInput(from), to: toDateInput(to) };
}

function fmtPct(rate) {
  if (rate === null || rate === undefined) return '—';
  return `${(rate * 100).toFixed(1)}%`;
}

function fmtScore(score) {
  if (score === null || score === undefined || score < 0) return '—';
  return score.toFixed(1);
}

// ── Main page ─────────────────────────────────────────────────────────────────

/**
 * HR Analytics Dashboard page.
 *
 * @returns {JSX.Element}
 */
export default function AnalyticsDashboardPage() {
  const { hasAnyRole } = useAuth();
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const isPrivileged = hasAnyRole([ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]);

  const [filters, setFilters] = useState(defaultFilters);

  // For EMPLOYEE, all filtering is handled server-side — no need to pass departmentId
  const effectiveFilters = isPrivileged ? filters : { from: filters.from, to: filters.to };

  const refreshAll = useRefreshAnalytics();

  // ── Data hooks ──────────────────────────────────────────────────────────────
  const summary = useAnalyticsSummary(effectiveFilters);
  const attendance = useAnalyticsAttendance(effectiveFilters);
  const leaves = useAnalyticsLeaves(effectiveFilters);
  const tasks = useAnalyticsTasks(effectiveFilters);
  const performance = useAnalyticsPerformance({ from: filters.from, to: filters.to }, isPrivileged);
  const departments = useAnalyticsDepartments(isPrivileged);

  // Use department data for filter dropdown
  const departmentOptions = useMemo(() => {
    if (!departments.data?.departments) return [];
    return departments.data.departments.map((d) => ({
      id: d.departmentId,
      name: d.departmentName,
    }));
  }, [departments.data]);

  const handleRefresh = useCallback(() => {
    refreshAll();
  }, [refreshAll]);

  const isLoadingSummary = summary.isLoading;
  const isAnyError = summary.isError || attendance.isError || leaves.isError || tasks.isError;
  const isFetching =
    summary.isFetching || attendance.isFetching || leaves.isFetching || tasks.isFetching;

  // ── Attendance trend chart data ─────────────────────────────────────────────
  const attendanceTrendData = useMemo(() => {
    if (!attendance.data?.trend) return [];
    return attendance.data.trend.map((p) => ({
      date: p.date.slice(5), // MM-DD
      present: p.present,
      absent: p.absent,
      rate: +(p.rate * 100).toFixed(1),
    }));
  }, [attendance.data]);

  // ── Leave type distribution data ────────────────────────────────────────────
  const leaveTypeData = useMemo(() => {
    if (!leaves.data?.byType) return [];
    return leaves.data.byType.map((b) => ({
      name: b.leaveType,
      value: b.count,
    }));
  }, [leaves.data]);

  // ── Task status data ────────────────────────────────────────────────────────
  const taskStatusData = useMemo(() => {
    if (!tasks.data?.statusBreakdown) return [];
    return tasks.data.statusBreakdown
      .filter((s) => s.count > 0)
      .map((s) => ({
        name: s.status.replace(/_/g, ' '),
        value: s.count,
        color: TASK_STATUS_COLORS[s.status] || '#9CA3AF',
      }));
  }, [tasks.data]);

  // ── Department headcount data ───────────────────────────────────────────────
  const deptHeadcountData = useMemo(() => {
    if (!departments.data?.departments) return [];
    return departments.data.departments.slice(0, 10).map((d) => ({
      name: d.departmentCode || d.departmentName.slice(0, 8),
      total: d.headcount,
      active: d.activeCount,
    }));
  }, [departments.data]);

  // ── AI score trend data ─────────────────────────────────────────────────────
  const aiScoreTrendData = useMemo(() => {
    if (!performance.data?.scoreTrend) return [];
    return performance.data.scoreTrend.map((p) => ({
      date: p.label.slice(5),
      score: +p.avgScore.toFixed(1),
      count: p.evaluationCount,
    }));
  }, [performance.data]);

  // ── Initial loading state ───────────────────────────────────────────────────
  if (isLoadingSummary) {
    return (
      <>
        <Helmet>
          <title>HR Analytics — PeopleCore HR</title>
        </Helmet>
        <Box
          sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}
        >
          <CircularProgress aria-label="Loading analytics" />
        </Box>
      </>
    );
  }

  // ── Top-level error state ───────────────────────────────────────────────────
  if (isAnyError && !summary.data) {
    return (
      <>
        <Helmet>
          <title>HR Analytics — PeopleCore HR</title>
        </Helmet>
        <Box sx={{ p: 2 }}>
          <Alert
            severity="error"
            action={
              <Button
                color="inherit"
                size="small"
                startIcon={<RefreshRoundedIcon />}
                onClick={handleRefresh}
              >
                Retry
              </Button>
            }
          >
            {summary.error?.message ?? 'Failed to load analytics data. Please try again.'}
          </Alert>
        </Box>
      </>
    );
  }

  const s = summary.data;

  return (
    <>
      <Helmet>
        <title>HR Analytics — PeopleCore HR</title>
      </Helmet>

      <Box sx={{ pb: 4 }}>
        {/* Page heading */}
        <Stack
          direction="row"
          alignItems="center"
          justifyContent="space-between"
          mb={3}
          flexWrap="wrap"
          gap={2}
        >
          <Box>
            <Typography
              variant="h4"
              fontWeight={700}
              sx={{ color: isDark ? '#F0EDE6' : '#1A2342', letterSpacing: '-0.01em' }}
            >
              HR Analytics
            </Typography>
            <Typography variant="body2" color="text.secondary" mt={0.5}>
              {isPrivileged ? 'Organisation-wide insights' : 'Your personal analytics'}
            </Typography>
          </Box>
          {isFetching && (
            <Stack direction="row" alignItems="center" gap={1}>
              <CircularProgress size={16} />
              <Typography variant="caption" color="text.secondary">
                Refreshing…
              </Typography>
            </Stack>
          )}
        </Stack>

        {/* Filters */}
        <AnalyticsFiltersBar
          filters={filters}
          onFiltersChange={setFilters}
          onRefresh={handleRefresh}
          isPrivileged={isPrivileged}
          departments={departmentOptions}
          isFetching={isFetching}
        />

        {/* ── KPI Cards ──────────────────────────────────────────────────── */}
        <Grid container spacing={2} mb={3}>
          {/* Employee count — only for privileged */}
          {isPrivileged && (
            <Grid item xs={12} sm={6} md={3}>
              <AnalyticsKpiCard
                icon={<PeopleRoundedIcon />}
                label="Total Employees"
                value={s?.totalEmployees ?? 0}
                subValue={`${s?.activeEmployees ?? 0} active · ${s?.employeesOnLeave ?? 0} on leave`}
                loading={isLoadingSummary}
                color={CHART_COLORS.primary}
              />
            </Grid>
          )}

          {/* Attendance rate */}
          <Grid item xs={12} sm={6} md={isPrivileged ? 3 : 4}>
            <AnalyticsKpiCard
              icon={<AccessTimeRoundedIcon />}
              label="Attendance Rate"
              value={fmtPct(s?.attendanceRate)}
              subValue={`${s?.presentCount ?? 0} present · ${s?.absentCount ?? 0} absent`}
              loading={isLoadingSummary}
              color={CHART_COLORS.success}
            />
          </Grid>

          {/* Task completion */}
          <Grid item xs={12} sm={6} md={isPrivileged ? 3 : 4}>
            <AnalyticsKpiCard
              icon={<TaskAltRoundedIcon />}
              label="Task Completion"
              value={fmtPct(s?.taskCompletionRate)}
              subValue={`${s?.completedTasks ?? 0} done · ${s?.overdueTasks ?? 0} overdue`}
              loading={isLoadingSummary}
              color={CHART_COLORS.secondary}
            />
          </Grid>

          {/* AI score — privileged only */}
          {isPrivileged && (
            <Grid item xs={12} sm={6} md={3}>
              <AnalyticsKpiCard
                icon={<AutoAwesomeRoundedIcon />}
                label="Avg AI Score"
                value={fmtScore(s?.avgAiScore)}
                subValue={`${s?.completedAiEvaluations ?? 0} evaluations`}
                loading={isLoadingSummary}
                color={CHART_COLORS.gold}
              />
            </Grid>
          )}

          {/* Leave requests */}
          <Grid item xs={12} sm={6} md={isPrivileged ? 3 : 4}>
            <AnalyticsKpiCard
              icon={<EventNoteRoundedIcon />}
              label="Leave Requests"
              value={s?.totalLeaveRequests ?? 0}
              subValue={`${s?.pendingLeaveRequests ?? 0} pending · ${s?.approvedLeaveRequests ?? 0} approved`}
              loading={isLoadingSummary}
              color={CHART_COLORS.warning}
            />
          </Grid>
        </Grid>

        {/* ── Charts Row 1: Attendance Trend + Leave Distribution ─────────── */}
        <Grid container spacing={3} mb={3}>
          {/* Attendance trend line chart */}
          <Grid item xs={12} md={8}>
            <ChartCard
              title="Attendance Trend"
              subtitle={`Daily present/absent counts · ${filters.from} – ${filters.to}`}
              loading={attendance.isLoading}
              isEmpty={attendanceTrendData.length === 0}
              emptyText="No attendance records for this period"
              height={280}
              showRefresh
              onRefresh={handleRefresh}
              isFetching={attendance.isFetching}
            >
              <ResponsiveContainer width="100%" height={280}>
                <LineChart
                  data={attendanceTrendData}
                  margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke={isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}
                  />
                  <XAxis
                    dataKey="date"
                    tick={{ fontSize: 11, fill: isDark ? '#94A3B8' : '#6B7280' }}
                  />
                  <YAxis tick={{ fontSize: 11, fill: isDark ? '#94A3B8' : '#6B7280' }} />
                  <Tooltip
                    contentStyle={{
                      background: isDark ? '#1A2342' : '#fff',
                      border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : '#e5e7eb'}`,
                      borderRadius: 8,
                      fontSize: 12,
                    }}
                  />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Line
                    type="monotone"
                    dataKey="present"
                    name="Present"
                    stroke={CHART_COLORS.success}
                    strokeWidth={2}
                    dot={false}
                  />
                  <Line
                    type="monotone"
                    dataKey="absent"
                    name="Absent"
                    stroke={CHART_COLORS.error}
                    strokeWidth={2}
                    dot={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </ChartCard>
          </Grid>

          {/* Leave distribution donut chart */}
          <Grid item xs={12} md={4}>
            <ChartCard
              title="Leave Distribution"
              subtitle="By leave type"
              loading={leaves.isLoading}
              isEmpty={leaveTypeData.length === 0}
              emptyText="No leave requests in this period"
              height={280}
              showRefresh
              onRefresh={handleRefresh}
              isFetching={leaves.isFetching}
            >
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={leaveTypeData}
                    cx="50%"
                    cy="50%"
                    innerRadius={65}
                    outerRadius={95}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {leaveTypeData.map((_, i) => (
                      <Cell
                        key={`cell-${i}`}
                        fill={LEAVE_TYPE_COLORS[i % LEAVE_TYPE_COLORS.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      background: isDark ? '#1A2342' : '#fff',
                      border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : '#e5e7eb'}`,
                      borderRadius: 8,
                      fontSize: 12,
                    }}
                  />
                  <Legend wrapperStyle={{ fontSize: 11 }} />
                </PieChart>
              </ResponsiveContainer>
            </ChartCard>
          </Grid>
        </Grid>

        {/* ── Charts Row 2: Task Status + Department Headcount ────────────── */}
        <Grid container spacing={3} mb={3}>
          {/* Task status bar chart */}
          <Grid item xs={12} md={isPrivileged ? 6 : 12}>
            <ChartCard
              title="Task Status"
              subtitle="Tasks by current status"
              loading={tasks.isLoading}
              isEmpty={taskStatusData.length === 0}
              emptyText="No tasks found"
              height={280}
              showRefresh
              onRefresh={handleRefresh}
              isFetching={tasks.isFetching}
            >
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={taskStatusData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke={isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}
                  />
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 10, fill: isDark ? '#94A3B8' : '#6B7280' }}
                  />
                  <YAxis tick={{ fontSize: 11, fill: isDark ? '#94A3B8' : '#6B7280' }} />
                  <Tooltip
                    contentStyle={{
                      background: isDark ? '#1A2342' : '#fff',
                      border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : '#e5e7eb'}`,
                      borderRadius: 8,
                      fontSize: 12,
                    }}
                  />
                  <Bar dataKey="value" name="Tasks" radius={[4, 4, 0, 0]}>
                    {taskStatusData.map((entry, i) => (
                      <Cell key={`cell-${i}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
          </Grid>

          {/* Department headcount — privileged only */}
          {isPrivileged && (
            <Grid item xs={12} md={6}>
              <ChartCard
                title="Department Headcount"
                subtitle="Employees per department"
                loading={departments.isLoading}
                isEmpty={deptHeadcountData.length === 0}
                emptyText="No department data available"
                height={280}
                showRefresh
                onRefresh={handleRefresh}
                isFetching={departments.isFetching}
              >
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart
                    data={deptHeadcountData}
                    layout="vertical"
                    margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                  >
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke={isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}
                      horizontal={false}
                    />
                    <XAxis
                      type="number"
                      tick={{ fontSize: 11, fill: isDark ? '#94A3B8' : '#6B7280' }}
                    />
                    <YAxis
                      type="category"
                      dataKey="name"
                      tick={{ fontSize: 11, fill: isDark ? '#94A3B8' : '#6B7280' }}
                      width={55}
                    />
                    <Tooltip
                      contentStyle={{
                        background: isDark ? '#1A2342' : '#fff',
                        border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : '#e5e7eb'}`,
                        borderRadius: 8,
                        fontSize: 12,
                      }}
                    />
                    <Legend wrapperStyle={{ fontSize: 12 }} />
                    <Bar
                      dataKey="total"
                      name="Total"
                      fill={CHART_COLORS.primary}
                      radius={[0, 4, 4, 0]}
                    />
                    <Bar
                      dataKey="active"
                      name="Active"
                      fill={CHART_COLORS.success}
                      radius={[0, 4, 4, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
              </ChartCard>
            </Grid>
          )}
        </Grid>

        {/* ── AI Performance Trend — privileged only ─────────────────────── */}
        {isPrivileged && (
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <ChartCard
                title="AI Performance Trend"
                subtitle="Average AI evaluation score over time"
                loading={performance.isLoading}
                isEmpty={aiScoreTrendData.length === 0}
                emptyText="No AI evaluations in this period"
                height={260}
                showRefresh
                onRefresh={handleRefresh}
                isFetching={performance.isFetching}
              >
                <ResponsiveContainer width="100%" height={260}>
                  <LineChart
                    data={aiScoreTrendData}
                    margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
                  >
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke={isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}
                    />
                    <XAxis
                      dataKey="date"
                      tick={{ fontSize: 11, fill: isDark ? '#94A3B8' : '#6B7280' }}
                    />
                    <YAxis
                      domain={[0, 100]}
                      tick={{ fontSize: 11, fill: isDark ? '#94A3B8' : '#6B7280' }}
                    />
                    <Tooltip
                      contentStyle={{
                        background: isDark ? '#1A2342' : '#fff',
                        border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : '#e5e7eb'}`,
                        borderRadius: 8,
                        fontSize: 12,
                      }}
                    />
                    <Legend wrapperStyle={{ fontSize: 12 }} />
                    <Line
                      type="monotone"
                      dataKey="score"
                      name="Avg Score"
                      stroke={CHART_COLORS.gold}
                      strokeWidth={2}
                      dot={{ r: 4 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </ChartCard>
            </Grid>
          </Grid>
        )}
      </Box>
    </>
  );
}
