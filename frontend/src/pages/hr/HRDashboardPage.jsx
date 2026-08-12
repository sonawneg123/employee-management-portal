/**
 * @fileoverview HRDashboardPage — HR/Manager-focused dashboard.
 *
 * Displays people-management metrics:
 * - KPI cards: Total Employees, New Hires This Month, Pending Leaves, On Leave Today
 * - Department distribution chart + Upcoming leaves widget
 * - Recent HR activity feed + Attendance summary widget
 *
 * All data is fetched via the existing dashboard hooks (real backend data).
 */

import React, { useCallback } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Grid,
} from '@mui/material';
import RefreshIcon    from '@mui/icons-material/Refresh';
import EventNoteIcon  from '@mui/icons-material/EventNote';
import PeopleIcon     from '@mui/icons-material/People';
import ApartmentIcon  from '@mui/icons-material/Apartment';

import { useDashboardSummary, useRefreshAllDashboard } from '@/hooks/useDashboard';
import { ROUTES } from '@/constants/routes';

import DashboardHeader             from '@/components/dashboard/DashboardHeader';
import DashboardSkeleton           from '@/components/dashboard/DashboardSkeleton';
import WelcomeCard                 from '@/components/dashboard/WelcomeCard';
import StatisticsCards             from '@/components/dashboard/StatisticsCards';
import DepartmentDistributionChart from '@/components/dashboard/DepartmentDistributionChart';
import UpcomingLeavesWidget        from '@/components/dashboard/UpcomingLeavesWidget';
import RecentActivity              from '@/components/dashboard/RecentActivity';
import AttendanceSummaryWidget     from '@/components/dashboard/AttendanceSummaryWidget';

/**
 * HR/Manager dashboard page — people-management view.
 *
 * @returns {JSX.Element}
 */
export default function HRDashboardPage() {
  const navigate = useNavigate();
  const {
    data:      summary,
    isLoading,
    isFetching,
    isError,
    error,
    refresh:   refreshSummary,
  } = useDashboardSummary();

  const refreshAll = useRefreshAllDashboard();
  const handleRefresh = useCallback(() => refreshAll(), [refreshAll]);

  // ── Loading ────────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <>
        <Helmet><title>HR Dashboard — Employee Portal</title></Helmet>
        <DashboardSkeleton />
      </>
    );
  }

  // ── Error ──────────────────────────────────────────────────────────────────
  if (isError) {
    return (
      <>
        <Helmet><title>HR Dashboard — Employee Portal</title></Helmet>
        <Box sx={{ p: 2 }}>
          <Alert
            severity="error"
            action={
              <Button color="inherit" size="small" startIcon={<RefreshIcon />} onClick={refreshSummary}>
                Retry
              </Button>
            }
          >
            {error?.message ?? 'Failed to load dashboard data.'}
          </Alert>
        </Box>
      </>
    );
  }

  // ── Data ───────────────────────────────────────────────────────────────────
  return (
    <>
      <Helmet><title>HR Dashboard — Employee Portal</title></Helmet>

      <Box sx={{ pb: 4 }}>
        <DashboardHeader
          lastUpdated={new Date().toISOString()}
          isFetching={isFetching}
          onRefresh={handleRefresh}
        />

        {/* Welcome banner */}
        <WelcomeCard />

        {/* KPI cards */}
        <StatisticsCards />

        {/* Quick actions */}
        <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap', mb: 3 }}>
          <Button
            variant="contained"
            startIcon={<PeopleIcon />}
            onClick={() => navigate(ROUTES.HR_EMPLOYEES)}
            size="small"
          >
            Manage Employees
          </Button>
          <Button
            variant="outlined"
            startIcon={<ApartmentIcon />}
            onClick={() => navigate(ROUTES.HR_DEPARTMENTS)}
            size="small"
          >
            Departments
          </Button>
          <Button
            variant="outlined"
            color="warning"
            startIcon={<EventNoteIcon />}
            onClick={() => navigate(ROUTES.HR_LEAVES)}
            size="small"
          >
            Review Leaves
            {summary?.pendingLeaves > 0 && ` (${summary.pendingLeaves})`}
          </Button>
        </Box>

        {/* Department distribution + upcoming leaves */}
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid size={{ xs: 12, md: 7 }}>
            <DepartmentDistributionChart />
          </Grid>
          <Grid size={{ xs: 12, md: 5 }}>
            <UpcomingLeavesWidget />
          </Grid>
        </Grid>

        {/* Activity + attendance */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, lg: 8 }}>
            <RecentActivity />
          </Grid>
          <Grid size={{ xs: 12, lg: 4 }}>
            <AttendanceSummaryWidget />
          </Grid>
        </Grid>
      </Box>
    </>
  );
}
