/**
 * @fileoverview ManagerLeavePage — Manager leave approval/rejection portal.
 *
 * Provides Managers with a dedicated view to:
 * - View all team leave requests
 * - Filter by PENDING / APPROVED / REJECTED / CANCELLED
 * - See employee name and profile photo
 * - See leave type, period, days, reason, and status
 * - Approve pending leave requests (triggers LEAVE_APPROVED notification)
 * - Reject pending leave requests with a reason (triggers LEAVE_REJECTED notification)
 *
 * Reuses the existing leave infrastructure:
 *   - LeavesPage component (all hooks, mutations, dialogs)
 *   - useLeaves / useApproveLeave / useRejectLeave hooks
 *   - LeaveApprovalDialog / RejectLeaveDialog components
 *   - LeaveTable with employee avatar (via leaveColumns.jsx)
 *   - NotificationBell (handles LEAVE_APPROVED + LEAVE_REJECTED sounds)
 *
 * Authorization: MANAGER, HR, ADMIN only (enforced by RoleProtectedRoute in AppRoutes).
 */

import React, { useCallback, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Card,
  Chip,
  Snackbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import HowToRegRoundedIcon from '@mui/icons-material/HowToRegRounded';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';
import { ROUTES } from '@/constants/routes';
import { getProfile } from '@/services/profileApi';
import {
  LEAVE_DEFAULT_PAGE_SIZE,
  LEAVE_DEFAULT_SORT,
  LEAVE_DEFAULT_DIRECTION,
} from '@/constants/leaveConstants';
import { useLeaves, useApproveLeave, useRejectLeave } from '@/hooks/useLeaveHooks';

import LeaveTable from '@/components/leaves/LeaveTable';
import LeaveToolbar from '@/components/leaves/LeaveToolbar';
import LeavePagination from '@/components/leaves/LeavePagination';
import LeaveApprovalDialog from '@/components/leaves/LeaveApprovalDialog';
import RejectLeaveDialog from '@/components/leaves/RejectLeaveDialog';
import LeaveStatistics from '@/components/leaves/LeaveStatistics';

/**
 * Dedicated leave management page for Managers.
 * Defaults the status filter to PENDING so Managers see actionable items first.
 *
 * @returns {JSX.Element}
 */
export default function ManagerLeavePage() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const { hasAnyRole } = useAuth();

  const canApprove = hasAnyRole([ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]);
  // Managers cannot edit leave requests (create/update) — only HR/Admin do
  const canEdit = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canCancel = false;

  // ── Query params — default to PENDING so managers see actionable requests ──
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(LEAVE_DEFAULT_PAGE_SIZE);
  const [sort, setSort] = useState(LEAVE_DEFAULT_SORT);
  const [direction, setDirection] = useState(LEAVE_DEFAULT_DIRECTION);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('PENDING');
  const [type, setType] = useState('');

  // ── Dialog state ────────────────────────────────────────────────────────────
  const [approveTarget, setApproveTarget] = useState(null);
  const [rejectTarget, setRejectTarget] = useState(null);

  // ── Snackbar ────────────────────────────────────────────────────────────────
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });
  const showSnackbar = useCallback(
    (severity, message) => setSnackbar({ open: true, severity, message }),
    [],
  );
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  // ── Data ────────────────────────────────────────────────────────────────────
  const { data, isLoading, isFetching, isError, error, refresh } = useLeaves({
    page,
    size: pageSize,
    sort,
    direction,
    search,
    status,
    type,
  });

  const approveMutation = useApproveLeave();
  const rejectMutation = useRejectLeave();

  const leaves = useMemo(() => data?.content ?? [], [data?.content]);
  const totalElements = data?.totalElements ?? 0;
  const hasFilters = Boolean(search || status || type);

  // ── Handlers ────────────────────────────────────────────────────────────────

  const handleSearchChange = useCallback((v) => {
    setSearch(v);
    setPage(0);
  }, []);
  const handleStatusChange = useCallback((v) => {
    setStatus(v);
    setPage(0);
  }, []);
  const handleTypeChange = useCallback((v) => {
    setType(v);
    setPage(0);
  }, []);
  const handleSortChange = useCallback((f, d) => {
    setSort(f);
    setDirection(d);
    setPage(0);
  }, []);
  const handleDirectionChange = useCallback((d) => {
    setDirection(d);
    setPage(0);
  }, []);
  const handleClearFilters = useCallback(() => {
    setSearch('');
    setStatus('');
    setType('');
    setPage(0);
  }, []);

  const handleView = useCallback((l) => navigate(ROUTES.LEAVE_DETAIL(l.id)), [navigate]);

  const handleApprove = useCallback(async () => {
    if (!approveTarget) return;
    try {
      await approveMutation.mutateAsync(approveTarget.id);
      showSnackbar('success', `Leave approved for ${approveTarget.employeeName ?? 'employee'}.`);
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to approve leave.');
    } finally {
      setApproveTarget(null);
    }
  }, [approveTarget, approveMutation, showSnackbar]);

  const handleReject = useCallback(
    async (reason) => {
      if (!rejectTarget) return;
      try {
        await rejectMutation.mutateAsync({ id: rejectTarget.id, reason });
        showSnackbar('success', `Leave rejected for ${rejectTarget.employeeName ?? 'employee'}.`);
      } catch (err) {
        showSnackbar('error', err?.message ?? 'Failed to reject leave.');
      } finally {
        setRejectTarget(null);
      }
    },
    [rejectTarget, rejectMutation, showSnackbar],
  );

  // Pending count badge
  const pendingCount = useMemo(
    () => (status === '' ? leaves.filter((l) => l.status === 'PENDING').length : totalElements),
    [leaves, status, totalElements],
  );

  return (
    <>
      <Helmet>
        <title>Leave Approvals — PeopleCore HR</title>
      </Helmet>

      {/* Page header */}
      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box
          sx={{
            width: 44,
            height: 44,
            borderRadius: '12px',
            bgcolor: 'rgba(16,185,129,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <HowToRegRoundedIcon sx={{ fontSize: 22, color: 'success.main' }} />
        </Box>
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Typography
              variant="h2"
              fontWeight={800}
              sx={{ letterSpacing: '-0.02em', mb: 0, lineHeight: 1.2 }}
            >
              Leave Approvals
            </Typography>
            {status === 'PENDING' && pendingCount > 0 && (
              <Chip
                label={`${pendingCount} pending`}
                size="small"
                color="warning"
                sx={{ fontWeight: 700, height: 22, fontSize: '0.72rem' }}
              />
            )}
          </Box>
          <Typography variant="body2" color="text.secondary">
            Review and approve or reject team leave requests
          </Typography>
        </Box>
      </Box>

      {/* Statistics */}
      <LeaveStatistics leaves={leaves} isLoading={isLoading} />

      <Card>
        <LeaveToolbar
          search={search}
          status={status}
          type={type}
          sort={sort}
          direction={direction}
          totalElements={totalElements}
          isFetching={isFetching}
          canCreate={false}
          onSearchChange={handleSearchChange}
          onStatusChange={handleStatusChange}
          onTypeChange={handleTypeChange}
          onSortChange={(f) => handleSortChange(f, direction)}
          onDirectionChange={handleDirectionChange}
          onRefresh={refresh}
          onClearFilters={handleClearFilters}
        />

        {!isMobile && (
          <LeaveTable
            leaves={leaves}
            isLoading={isLoading}
            isFetching={isFetching}
            isError={isError}
            error={error}
            sort={sort}
            direction={direction}
            hasFilters={hasFilters}
            canApprove={canApprove}
            canEdit={canEdit}
            canCancel={canCancel}
            onSort={handleSortChange}
            onView={handleView}
            onApprove={(l) => setApproveTarget(l)}
            onReject={(l) => setRejectTarget(l)}
            onEdit={() => {}}
            onCancel={() => {}}
            onRetry={refresh}
            onClearFilters={handleClearFilters}
            canCreate={false}
          />
        )}

        {isMobile && (
          <Box sx={{ p: 2 }}>
            {isLoading
              ? Array.from({ length: 5 }, (_, i) => (
                  <Box
                    key={i}
                    sx={{ mb: 1.5, height: 90, bgcolor: 'action.hover', borderRadius: 2 }}
                  />
                ))
              : leaves.map((l) => (
                  <Box
                    key={l.id}
                    onClick={() => handleView(l)}
                    sx={{
                      mb: 1.5,
                      p: 2,
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 2,
                      cursor: 'pointer',
                      '&:hover': { bgcolor: 'action.hover' },
                    }}
                  >
                    <Typography variant="body2" fontWeight={600}>
                      {l.employeeName ?? '—'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {l.leaveType} · {l.startDate} → {l.endDate} · {l.totalDays}d · {l.status}
                    </Typography>
                    {l.status === 'PENDING' && canApprove && (
                      <Box sx={{ mt: 1, display: 'flex', gap: 1 }}>
                        <Chip
                          label="Approve"
                          color="success"
                          size="small"
                          clickable
                          onClick={(e) => {
                            e.stopPropagation();
                            setApproveTarget(l);
                          }}
                          sx={{ fontWeight: 600 }}
                        />
                        <Chip
                          label="Reject"
                          color="error"
                          size="small"
                          clickable
                          onClick={(e) => {
                            e.stopPropagation();
                            setRejectTarget(l);
                          }}
                          sx={{ fontWeight: 600 }}
                        />
                      </Box>
                    )}
                  </Box>
                ))}
          </Box>
        )}

        <LeavePagination
          page={page}
          pageSize={pageSize}
          totalElements={totalElements}
          onPageChange={setPage}
          onPageSizeChange={(s) => {
            setPageSize(s);
            setPage(0);
          }}
          disabled={isLoading || isFetching}
        />
      </Card>

      {/* Approval dialog */}
      <LeaveApprovalDialog
        open={Boolean(approveTarget)}
        leave={approveTarget}
        isApproving={approveMutation.isPending}
        onConfirm={handleApprove}
        onCancel={() => setApproveTarget(null)}
      />

      {/* Rejection dialog with reason */}
      <RejectLeaveDialog
        open={Boolean(rejectTarget)}
        leave={rejectTarget}
        isRejecting={rejectMutation.isPending}
        onConfirm={handleReject}
        onCancel={() => setRejectTarget(null)}
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
