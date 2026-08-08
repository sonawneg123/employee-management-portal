/**
 * @fileoverview MyLeavesPage — self-service leave page for individual employees.
 *
 * Shows the current user's own leave requests with:
 * - {@link LeaveBalanceCard}   — entitlement vs used progress bars
 * - {@link LeaveTimeline}      — chronological leave history
 * - {@link LeaveCalendar}      — monthly calendar of own leaves
 * - Submit new leave request dialog
 * - Cancel pending requests
 *
 * Accessible to all roles; data is filtered server-side by the current user.
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
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
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';

import { ROUTES }          from '@/constants/routes';
import { useMyLeaves, useCreateLeave, useDeleteLeave } from '@/hooks/useLeaveHooks';

import LeaveBalanceCard    from '@/components/leaves/LeaveBalanceCard';
import LeaveTimeline       from '@/components/leaves/LeaveTimeline';
import LeaveCalendar       from '@/components/leaves/LeaveCalendar';
import LeaveDialog         from '@/components/leaves/LeaveDialog';
import LeaveFilters        from '@/components/leaves/LeaveFilters';
import LeaveStatistics     from '@/components/leaves/LeaveStatistics';

/**
 * My Leaves self-service page.
 *
 * @returns {JSX.Element}
 */
export default function MyLeavesPage() {
  const navigate = useNavigate();
  const [tab,    setTab]    = useState(0); // 0=Timeline, 1=Calendar
  const [status, setStatus] = useState('');
  const [type,   setType]   = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [snackbar,   setSnackbar]   = useState({ open: false, severity: 'success', message: '' });

  const showSnackbar  = useCallback((severity, message) => setSnackbar({ open: true, severity, message }), []);
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  const {
    data, isLoading, isError, error, refresh,
  } = useMyLeaves({ status, type, size: 100 }); // large page for calendar/timeline

  const createMutation = useCreateLeave();
  const deleteMutation = useDeleteLeave();

  const allLeaves    = data?.content ?? [];
  const approved     = allLeaves.filter((l) => l.status === 'APPROVED');

  const handleSubmit = useCallback(async (payload) => {
    try {
      await createMutation.mutateAsync(payload);
      showSnackbar('success', 'Leave request submitted successfully.');
      setDialogOpen(false);
      refresh();
    } catch (err) {
      if (!err?.violations) showSnackbar('error', err?.message ?? 'Failed to submit request.');
    }
  }, [createMutation, showSnackbar, refresh]);

  const handleCancel = useCallback(async (leave) => {
    try {
      await deleteMutation.mutateAsync(leave.id);
      showSnackbar('success', 'Leave request cancelled.');
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to cancel leave.');
    }
  }, [deleteMutation, showSnackbar]);

  return (
    <>
      <Helmet><title>My Leaves — Employee Portal</title></Helmet>

      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3, flexWrap: 'wrap', gap: 1 }}>
        <Box>
          <Typography variant="h4" fontWeight={700}>My Leaves</Typography>
          <Typography variant="body2" color="text.secondary">Your leave requests and balance</Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setDialogOpen(true)}
          aria-label="Request new leave"
        >
          Request Leave
        </Button>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
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

            <Card variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Filter
              </Typography>
              <LeaveFilters
                status={status} type={type}
                onStatusChange={setStatus} onTypeChange={setType}
              />
            </Card>
          </Box>
        </Grid>

        {/* Right: timeline / calendar tabs */}
        <Grid size={{ xs: 12, lg: 8 }}>
          <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
            <Tabs value={tab} onChange={(_e, v) => setTab(v)} aria-label="Leave view tabs">
              <Tab label="Timeline" id="tab-timeline" aria-controls="tabpanel-timeline" />
              <Tab label="Calendar" id="tab-calendar" aria-controls="tabpanel-calendar" />
            </Tabs>
          </Box>

          {tab === 0 && (
            <Box role="tabpanel" id="tabpanel-timeline" aria-labelledby="tab-timeline">
              <LeaveTimeline
                leaves={allLeaves.filter((l) => (!status || l.status === status) && (!type || l.leaveType === type))}
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

      <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert onClose={closeSnackbar} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
