/**
 * @fileoverview MyLeavesPage — self-service leave page for individual employees.
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Card,
  Grid,
  Snackbar,
  Tab,
  Tabs,
  Typography,
  alpha,
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import BeachAccessRoundedIcon from '@mui/icons-material/BeachAccessRounded';

import { ROUTES } from '@/constants/routes';
import { useMyLeaves, useCreateLeave } from '@/hooks/useLeaveHooks';
import { getProfile } from '@/services/profileApi';
import { useAuth } from '@/contexts/AuthContext';

import LeaveBalanceCard from '@/components/leaves/LeaveBalanceCard';
import LeaveTimeline from '@/components/leaves/LeaveTimeline';
import LeaveCalendar from '@/components/leaves/LeaveCalendar';
import LeaveDialog from '@/components/leaves/LeaveDialog';
import LeaveFilters from '@/components/leaves/LeaveFilters';
import LeaveStatistics from '@/components/leaves/LeaveStatistics';

/**
 * My Leaves self-service page.
 *
 * @returns {JSX.Element}
 */
export default function MyLeavesPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [tab, setTab] = useState(0); // 0=Timeline, 1=Calendar
  const [status, setStatus] = useState('');
  const [type, setType] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });

  const showSnackbar = useCallback(
    (severity, message) => setSnackbar({ open: true, severity, message }),
    [],
  );
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  // Fetch own profile to get employeeId (required when submitting leave)
  const { data: profile } = useQuery({
    queryKey: ['profile', user?.userId],
    queryFn: getProfile,
    enabled: Boolean(user?.userId),
    staleTime: 5 * 60_000,
  });

  const { data, isLoading, isError, error, refresh } = useMyLeaves({ status, type, size: 100 }); // large page for calendar/timeline

  const createMutation = useCreateLeave();

  const allLeaves = data?.content ?? [];
  const approved = allLeaves.filter((l) => l.status === 'APPROVED');

  const handleSubmit = useCallback(
    async (formPayload) => {
      if (!profile?.employeeId) {
        showSnackbar(
          'error',
          'Unable to submit leave: your employee record could not be found. Contact HR.',
        );
        return;
      }
      const payload = { ...formPayload, employeeId: profile.employeeId };
      try {
        await createMutation.mutateAsync(payload);
        showSnackbar('success', 'Leave request submitted successfully.');
        setDialogOpen(false);
        refresh();
      } catch (err) {
        if (!err?.violations) showSnackbar('error', err?.message ?? 'Failed to submit request.');
      }
    },
    [createMutation, showSnackbar, refresh, profile],
  );

  return (
    <>
      <Helmet>
        <title>My Leaves — PeopleCore HR</title>
      </Helmet>

      {/* Page header */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          mb: 4,
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: '12px',
              bgcolor: (t) => alpha(t.palette.primary.main, 0.1),
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'primary.main',
            }}
          >
            <BeachAccessRoundedIcon />
          </Box>
          <Box>
            <Typography variant="h2" fontWeight={800} sx={{ letterSpacing: '-0.02em' }}>
              My Leaves
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Your leave requests and balance
            </Typography>
          </Box>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddRoundedIcon />}
          onClick={() => setDialogOpen(true)}
          aria-label="Request new leave"
          sx={{ height: 44, borderRadius: '10px', px: 3, fontWeight: 600 }}
        >
          Request Leave
        </Button>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 3, borderRadius: '10px' }}>
          {error?.message ?? 'Failed to load your leave requests.'}
        </Alert>
      )}

      {/* Statistics */}
      <LeaveStatistics leaves={allLeaves} isLoading={isLoading} />

      <Grid container spacing={3}>
        {/* Left: balance + filters + view */}
        <Grid size={{ xs: 12, lg: 4 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <LeaveBalanceCard approvedLeaves={approved} isLoading={isLoading} />

            <Card
              sx={{
                p: 2.5,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '12px',
              }}
            >
              <Typography
                variant="overline"
                color="text.secondary"
                sx={{ fontWeight: 700, letterSpacing: '0.08em', fontSize: '0.7rem' }}
                gutterBottom
              >
                Filter Leaves
              </Typography>
              <LeaveFilters
                status={status}
                type={type}
                onStatusChange={setStatus}
                onTypeChange={setType}
              />
            </Card>
          </Box>
        </Grid>

        {/* Right: timeline / calendar tabs */}
        <Grid size={{ xs: 12, lg: 8 }}>
          <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
            <Tabs
              value={tab}
              onChange={(_e, v) => setTab(v)}
              aria-label="Leave view tabs"
              sx={{ '& .MuiTab-root': { fontWeight: 600, minWidth: 100 } }}
            >
              <Tab label="Timeline" id="tab-timeline" aria-controls="tabpanel-timeline" />
              <Tab label="Calendar" id="tab-calendar" aria-controls="tabpanel-calendar" />
            </Tabs>
          </Box>

          {tab === 0 && (
            <Box role="tabpanel" id="tabpanel-timeline" aria-labelledby="tab-timeline">
              <LeaveTimeline
                leaves={allLeaves.filter(
                  (l) => (!status || l.status === status) && (!type || l.leaveType === type),
                )}
                isLoading={isLoading}
              />
            </Box>
          )}

          {tab === 1 && (
            <Box role="tabpanel" id="tabpanel-calendar" aria-labelledby="tab-calendar">
              <LeaveCalendar
                leaves={allLeaves}
                onLeaveClick={(l) => navigate(ROUTES.LEAVE_DETAIL(l.id))}
              />
            </Box>
          )}
        </Grid>
      </Grid>

      <LeaveDialog
        open={dialogOpen}
        mode="create"
        isSubmitting={createMutation.isPending}
        serverErrors={createMutation.error?.violations}
        onSubmit={handleSubmit}
        onClose={() => setDialogOpen(false)}
      />

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={closeSnackbar}
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
