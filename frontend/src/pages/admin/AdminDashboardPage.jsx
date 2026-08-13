/**
 * @fileoverview AdminDashboardPage — full-visibility dashboard for ROLE_ADMIN users.
 *
 * Displays the complete operational picture:
 * - Welcome banner
 * - Four KPI cards: Total Employees, Active Departments, Pending Leaves, Present Today
 * - Quick action buttons
 * - Department distribution + Employee status charts
 * - Recent activity + upcoming leaves widgets
 *
 * All data fetched via existing dashboard hooks (real backend data, no mocks).
 */

import React, { useCallback } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Grid, Typography } from '@mui/material';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';
import PersonAddRoundedIcon from '@mui/icons-material/PersonAddRounded';
import ApartmentRoundedIcon from '@mui/icons-material/ApartmentRounded';
import EventNoteRoundedIcon from '@mui/icons-material/EventNoteRounded';
import AssessmentRoundedIcon from '@mui/icons-material/AssessmentRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import ManageAccountsRoundedIcon from '@mui/icons-material/ManageAccountsRounded';

import { useDashboardSummary, useRefreshAllDashboard } from '@/hooks/useDashboard';
import { ROUTES } from '@/constants/routes';

import DashboardHeader from '@/components/dashboard/DashboardHeader';
import DashboardSkeleton from '@/components/dashboard/DashboardSkeleton';
import WelcomeCard from '@/components/dashboard/WelcomeCard';
import StatisticsCards from '@/components/dashboard/StatisticsCards';
import DepartmentDistributionChart from '@/components/dashboard/DepartmentDistributionChart';
import EmployeeStatusChart from '@/components/dashboard/EmployeeStatusChart';
import RecentActivity from '@/components/dashboard/RecentActivity';
import UpcomingLeavesWidget from '@/components/dashboard/UpcomingLeavesWidget';
import AttendanceSummaryWidget from '@/components/dashboard/AttendanceSummaryWidget';

/**
 * @returns {JSX.Element}
 */
export default function AdminDashboardPage() {
  const navigate = useNavigate();
  const {
    data: summary,
    isLoading,
    isFetching,
    isError,
    error,
    refresh: refreshSummary,
  } = useDashboardSummary();

  const refreshAll = useRefreshAllDashboard();
  const handleRefresh = useCallback(() => refreshAll(), [refreshAll]);

  if (isLoading) {
    return (
      <>
        <Helmet>
          <title>Dashboard — PeopleCore HR</title>
        </Helmet>
        <DashboardSkeleton />
      </>
    );
  }

  if (isError) {
    return (
      <>
        <Helmet>
          <title>Dashboard — PeopleCore HR</title>
        </Helmet>
        <Box sx={{ p: 2 }}>
          <Alert
            severity="error"
            action={
              <Button
                color="inherit"
                size="small"
                startIcon={<RefreshRoundedIcon />}
                onClick={refreshSummary}
              >
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

  return (
    <>
      <Helmet>
        <title>Dashboard — PeopleCore HR</title>
      </Helmet>

      <Box sx={{ pb: 4 }}>
        <DashboardHeader
          lastUpdated={new Date().toISOString()}
          isFetching={isFetching}
          onRefresh={handleRefresh}
        />

        {/* Welcome */}
        <WelcomeCard />

        {/* KPI cards */}
        <StatisticsCards />

        {/* Quick actions */}
        <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap', mb: 3 }}>
          <Typography variant="overline" color="text.secondary" sx={{ width: '100%', mb: -0.5 }}>
            Quick actions
          </Typography>
          <Button
            variant="contained"
            startIcon={<PersonAddRoundedIcon />}
            onClick={() => navigate(ROUTES.ADMIN_EMPLOYEES)}
            size="small"
          >
            Employees
          </Button>
          <Button
            variant="outlined"
            startIcon={<ApartmentRoundedIcon />}
            onClick={() => navigate(ROUTES.ADMIN_DEPARTMENTS)}
            size="small"
          >
            Departments
          </Button>
          <Button
            variant="outlined"
            color="warning"
            startIcon={<EventNoteRoundedIcon />}
            onClick={() => navigate(ROUTES.ADMIN_LEAVES)}
            size="small"
          >
            Leaves{summary?.pendingLeaves > 0 && ` (${summary.pendingLeaves})`}
          </Button>
          <Button
            variant="outlined"
            startIcon={<AccessTimeRoundedIcon />}
            onClick={() => navigate(ROUTES.ADMIN_ATTENDANCE)}
            size="small"
          >
            Attendance
          </Button>
          <Button
            variant="outlined"
            startIcon={<AssessmentRoundedIcon />}
            onClick={() => navigate(ROUTES.ADMIN_REVIEWS)}
            size="small"
          >
            Reviews
          </Button>
          <Button
            variant="outlined"
            color="secondary"
            startIcon={<ManageAccountsRoundedIcon />}
            onClick={() => navigate(ROUTES.ADMIN_USERS)}
            size="small"
          >
            Users
          </Button>
        </Box>

        {/* Charts */}
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
