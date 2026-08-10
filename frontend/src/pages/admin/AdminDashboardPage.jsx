/**
 * @fileoverview AdminDashboardPage — full-visibility dashboard for ROLE_ADMIN users.
 *
 * Displays the complete operational picture:
 * - Four KPI cards: Total Employees, Active Employees, Total Departments, Pending Leaves
 * - Department distribution chart + Employee status chart
 * - Recent activity feed
 * - Quick action buttons: Add Employee, Manage Departments, Approve Leaves
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
import PersonAddIcon  from '@mui/icons-material/PersonAdd';
import ApartmentIcon  from '@mui/icons-material/Apartment';
import EventNoteIcon  from '@mui/icons-material/EventNote';

import { useDashboardSummary, useRefreshAllDashboard } from '@/hooks/useDashboard';
import { ROUTES } from '@/constants/routes';

import DashboardHeader              from '@/components/dashboard/DashboardHeader';
import DashboardSkeleton            from '@/components/dashboard/DashboardSkeleton';
import WelcomeCard                  from '@/components/dashboard/WelcomeCard';
import StatisticsCards              from '@/components/dashboard/StatisticsCards';
import DepartmentDistributionChart  from '@/components/dashboard/DepartmentDistributionChart';
import EmployeeStatusChart          from '@/components/dashboard/EmployeeStatusChart';
import RecentActivity               from '@/components/dashboard/RecentActivity';
import UpcomingLeavesWidget         from '@/components/dashboard/UpcomingLeavesWidget';
import AttendanceSummaryWidget      from '@/components/dashboard/AttendanceSummaryWidget';

/**
 * Admin dashboard page — full operational view.
 *
 * @returns {JSX.Element}
 */
export default function AdminDashboardPage() {
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
        <Helmet><title>Admin Dashboard — Employee Portal</title></Helmet>
        <DashboardSkeleton />
      </>
    );
  }

  // ── Error ──────────────────────────────────────────────────────────────────
  if (isError) {
    return (
      <>
        <Helmet><title>Admin Dashboard — Employee Portal</title></Helmet>
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
      <Helmet><title>Admin Dashboard — Employee Portal</title></Helmet>

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
            startIcon={<PersonAddIcon />}
            onClick={() => navigate(ROUTES.ADMIN_EMPLOYEES)}
            size="small"
          >
            Add Employee
          </Button>
          <Button
            variant="outlined"
            startIcon={<ApartmentIcon />}
            onClick={() => navigate(ROUTES.ADMIN_DEPARTMENTS)}
            size="small"
          >
            Add Department
          </Button>
          <Button
            variant="outlined"
            color="warning"
            startIcon={<EventNoteIcon />}
            onClick={() => navigate(ROUTES.ADMIN_LEAVES)}
            size="small"
          >
            Approve Leaves
            {summary?.pendingLeaves > 0 && ` (${summary.pendingLeaves})`}
          </Button>
        </Box>

        {/* Charts row */}
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid size={{ xs: 12, md: 6 }}>
            <DepartmentDistributionChart />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <EmployeeStatusChart />
          </Grid>
        </Grid>

        {/* Activity + widgets */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, lg: 8 }}>
            <RecentActivity />
          </Grid>
          <Grid size={{ xs: 12, lg: 4 }}>
            <Grid container spacing={3} direction="column">
              <Grid size={{ xs: 12 }}>
                <UpcomingLeavesWidget />
              </Grid>
              <Grid size={{ xs: 12 }}>
                <AttendanceSummaryWidget />
              </Grid>
            </Grid>
          </Grid>
        </Grid>
      </Box>
    </>
  );
}
