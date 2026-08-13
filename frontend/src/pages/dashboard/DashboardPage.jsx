/**
 * @fileoverview DashboardPage — the main dashboard view of the portal.
 *
 * Orchestrates the three dashboard data hooks ({@link useDashboardSummary},
 * {@link useDashboardActivity}, {@link useDashboardCharts}) and delegates
 * layout rendering to {@link RoleDashboard}. The page handles:
 *
 * - Initial loading state → {@link DashboardSkeleton}
 * - Empty-data state → {@link EmptyDashboard}
 * - Full error state → inline alert with retry action
 * - Live data state → {@link DashboardHeader} + {@link RoleDashboard}
 *
 * The document title is set via react-helmet-async.
 */

import React, { useCallback } from 'react';
import { Helmet } from 'react-helmet-async';
import { Alert, Box, Button } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';

import { useDashboardSummary, useRefreshAllDashboard } from '@/hooks/useDashboard';
import DashboardHeader from '@/components/dashboard/DashboardHeader';
import DashboardSkeleton from '@/components/dashboard/DashboardSkeleton';
import EmptyDashboard from '@/components/dashboard/EmptyDashboard';
import RoleDashboard from '@/components/dashboard/RoleDashboard';

// ── Component ─────────────────────────────────────────────────────────────────

/**
 * Main dashboard page.
 *
 * Reads the summary query to determine loading / empty / error states, then
 * hands off to RoleDashboard once data is available.
 *
 * @returns {JSX.Element}
 */
export default function DashboardPage() {
  const {
    data: summary,
    isLoading,
    isFetching,
    isError,
    error,
    refresh: refreshSummary,
  } = useDashboardSummary();

  const refreshAll = useRefreshAllDashboard();

  /**
   * Triggers a full refresh of all three dashboard query caches.
   *
   * @type {() => void}
   */
  const handleRefresh = useCallback(() => {
    refreshAll();
  }, [refreshAll]);

  // ── Loading state ──────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <>
        <Helmet>
          <title>Dashboard — Employee Portal</title>
        </Helmet>
        <DashboardSkeleton />
      </>
    );
  }

  // ── Error state ────────────────────────────────────────────────────────────

  if (isError) {
    return (
      <>
        <Helmet>
          <title>Dashboard — Employee Portal</title>
        </Helmet>
        <Box sx={{ p: 2 }}>
          <Alert
            severity="error"
            action={
              <Button
                color="inherit"
                size="small"
                startIcon={<RefreshIcon />}
                onClick={refreshSummary}
              >
                Retry
              </Button>
            }
          >
            {error?.message ?? 'Failed to load dashboard data. Please try again.'}
          </Alert>
        </Box>
      </>
    );
  }

  // ── Empty state ────────────────────────────────────────────────────────────

  const isEmpty = !summary || (summary.totalEmployees === 0 && summary.totalDepartments === 0);

  if (isEmpty) {
    return (
      <>
        <Helmet>
          <title>Dashboard — Employee Portal</title>
        </Helmet>
        <EmptyDashboard />
      </>
    );
  }

  // ── Live data state ────────────────────────────────────────────────────────

  return (
    <>
      <Helmet>
        <title>Dashboard — Employee Portal</title>
      </Helmet>

      <Box sx={{ pb: 4 }}>
        {/* Page header — title, live indicator, global refresh */}
        <DashboardHeader
          lastUpdated={new Date().toISOString()}
          isFetching={isFetching}
          onRefresh={handleRefresh}
        />

        {/* Role-aware layout — selects the correct widget combination */}
        <RoleDashboard />
      </Box>
    </>
  );
}
