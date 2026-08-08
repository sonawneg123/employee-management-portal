/**
 * @fileoverview LeavesPage — main leave management list view (Admin/HR/Manager).
 *
 * Manages all query-param state and renders the full leave table with:
 * - Search, type/status filters, sort, pagination
 * - Create leave dialog
 * - Approve / Reject / Edit / Cancel actions
 * - Leave statistics summary
 * - CSV export + snackbar notifications
 *
 * Role guards:
 *   ADMIN / HR    → full CRUD + approve/reject
 *   MANAGER       → approve/reject team requests, read-only create
 *   EMPLOYEE      → create own, cancel pending own
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import {
  Alert, Box, Card, Snackbar, Tab, Tabs, Typography, useMediaQuery, useTheme,
} from '@mui/material';
import { useAuth }         from '@/contexts/AuthContext';
import { ROLES }           from '@/constants/roles';
import { ROUTES }          from '@/constants/routes';
import {
  LEAVE_DEFAULT_PAGE_SIZE, LEAVE_DEFAULT_SORT, LEAVE_DEFAULT_DIRECTION,
  LEAVE_CSV_HEADERS, LEAVE_CSV_FIELDS,
} from '@/constants/leaveConstants';
import {
  useLeaves, useCreateLeave, useUpdateLeave,
  useDeleteLeave, useApproveLeave, useRejectLeave,
} from '@/hooks/useLeaveHooks';
import { buildLeaveCsvString, downloadLeaveCsv } from '@/utils/leaveFormatters';

import LeaveToolbar         from '@/components/leaves/LeaveToolbar';
import LeaveTable           from '@/components/leaves/LeaveTable';
import LeaveCard            from '@/components/leaves/LeaveCard';
import LeavePagination      from '@/components/leaves/LeavePagination';
import LeaveDialog          from '@/components/leaves/LeaveDialog';
import LeaveApprovalDialog  from '@/components/leaves/LeaveApprovalDialog';
import RejectLeaveDialog    from '@/components/leaves/RejectLeaveDialog';
import LeaveStatistics      from '@/components/leaves/LeaveStatistics';

/**
 * Leave management list page.
 *
 * @returns {JSX.Element}
 */
export default function LeavesPage() {
  const theme    = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const { hasAnyRole } = useAuth();

  const canCreate  = true; // all roles can submit
  const canApprove = hasAnyRole([ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]);
  const canEdit    = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canCancel  = true; // employees cancel own; guard by status in table

  // ── Query params ───────────────────────────────────────────────────────────
  const [page,      setPage]      = useState(0);
  const [pageSize,  setPageSize]  = useState(LEAVE_DEFAULT_PAGE_SIZE);
  const [sort,      setSort]      = useState(LEAVE_DEFAULT_SORT);
  const [direction, setDirection] = useState(LEAVE_DEFAULT_DIRECTION);
  const [search,    setSearch]    = useState('');
  const [status,    setStatus]    = useState('');
  const [type,      setType]      = useState('');

  // ── Dialog state ───────────────────────────────────────────────────────────
  const [dialogMode,    setDialogMode]    = useState(null);
  const [editLeave,     setEditLeave]     = useState(null);
  const [approveTarget, setApproveTarget] = useState(null);
  const [rejectTarget,  setRejectTarget]  = useState(null);

  // ── Snackbar ───────────────────────────────────────────────────────────────
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });
  const showSnackbar  = useCallback((severity, message) => setSnackbar({ open: true, severity, message }), []);
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  // ── Data ───────────────────────────────────────────────────────────────────
  const {
    data, isLoading, isFetching, isError, error, refresh,
  } = useLeaves({ page, size: pageSize, sort, direction, search, status, type });

  const createMutation  = useCreateLeave();
  const updateMutation  = useUpdateLeave();
  const deleteMutation  = useDeleteLeave();
  const approveMutation = useApproveLeave();
  const rejectMutation  = useRejectLeave();

  const leaves        = data?.content       ?? [];
  const totalElements = data?.totalElements ?? 0;
  const hasFilters    = Boolean(search || status || type);

  // ── Handlers ───────────────────────────────────────────────────────────────

  const handleSearchChange    = useCallback((v) => { setSearch(v);  setPage(0); }, []);
  const handleStatusChange    = useCallback((v) => { setStatus(v);  setPage(0); }, []);
  const handleTypeChange      = useCallback((v) => { setType(v);    setPage(0); }, []);
  const handleSortChange      = useCallback((f, d) => { setSort(f); setDirection(d); setPage(0); }, []);
  const handleDirectionChange = useCallback((d) => { setDirection(d); setPage(0); }, []);
  const handleClearFilters    = useCallback(() => { setSearch(''); setStatus(''); setType(''); setPage(0); }, []);

  const handleView   = useCallback((l) => navigate(ROUTES.LEAVE_DETAIL(l.id)), [navigate]);
  const handleAdd    = useCallback(() => { setEditLeave(null); setDialogMode('create'); }, []);
  const handleEdit   = useCallback((l) => { setEditLeave(l); setDialogMode('edit'); }, []);
  const handleClose  = useCallback(() => { setDialogMode(null); setEditLeave(null); }, []);

  const handleSubmit = useCallback(async (payload) => {
    try {
      if (dialogMode === 'create') {
        await createMutation.mutateAsync(payload);
        showSnackbar('success', 'Leave request submitted.');
      } else {
        await updateMutation.mutateAsync({ id: editLeave.id, payload });
        showSnackbar('success', 'Leave request updated.');
      }
      handleClose();
    } catch (err) {
      if (!err?.violations) showSnackbar('error', err?.message ?? 'An error occurred.');
    }
  }, [dialogMode, editLeave, createMutation, updateMutation, showSnackbar, handleClose]);

  const handleCancel = useCallback(async (leave) => {
    try {
      await deleteMutation.mutateAsync(leave.id);
      showSnackbar('success', 'Leave request cancelled.');
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to cancel leave.');
    }
  }, [deleteMutation, showSnackbar]);

  const handleApprove = useCallback(async () => {
    if (!approveTarget) return;
    try {
      await approveMutation.mutateAsync(approveTarget.id);
      showSnackbar('success', 'Leave request approved.');
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to approve leave.');
    } finally {
      setApproveTarget(null);
    }
  }, [approveTarget, approveMutation, showSnackbar]);

  const handleReject = useCallback(async (reason) => {
    if (!rejectTarget) return;
    try {
      await rejectMutation.mutateAsync({ id: rejectTarget.id, reason });
      showSnackbar('success', 'Leave request rejected.');
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to reject leave.');
    } finally {
      setRejectTarget(null);
    }
  }, [rejectTarget, rejectMutation, showSnackbar]);

  const handleExport = useCallback(() => {
    if (!leaves.length) return;
    const csv = buildLeaveCsvString(leaves, LEAVE_CSV_HEADERS, LEAVE_CSV_FIELDS);
    downloadLeaveCsv(csv, `leaves-page-${page + 1}.csv`);
    showSnackbar('success', `Exported ${leaves.length} records.`);
  }, [leaves, page, showSnackbar]);

  const serverErrors = (createMutation.error?.violations ?? updateMutation.error?.violations) || undefined;

  return (
    <>
      <Helmet><title>Leave Requests — Employee Portal</title></Helmet>

      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Leave Requests</Typography>
        <Typography variant="body2" color="text.secondary">Manage employee leave requests</Typography>
      </Box>

      {/* Statistics banner */}
      <LeaveStatistics leaves={leaves} isLoading={isLoading} />

      <Card variant="outlined">
        <LeaveToolbar
          search={search} status={status} type={type}
          sort={sort} direction={direction}
          totalElements={totalElements} isFetching={isFetching} canCreate={canCreate}
          onSearchChange={handleSearchChange} onStatusChange={handleStatusChange}
          onTypeChange={handleTypeChange} onSortChange={(f) => handleSortChange(f, direction)}
          onDirectionChange={handleDirectionChange}
          onAdd={handleAdd} onRefresh={refresh} onExport={handleExport}
          onClearFilters={handleClearFilters}
        />

        {/* Desktop table */}
        {!isMobile && (
          <LeaveTable
            leaves={leaves} isLoading={isLoading} isFetching={isFetching}
            isError={isError} error={error}
            sort={sort} direction={direction} hasFilters={hasFilters}
            canApprove={canApprove} canEdit={canEdit} canCancel={canCancel}
            onSort={handleSortChange} onView={handleView}
            onApprove={(l) => setApproveTarget(l)} onReject={(l) => setRejectTarget(l)}
            onEdit={handleEdit} onCancel={handleCancel}
            onRetry={refresh} onClearFilters={handleClearFilters}
            onAdd={handleAdd} canCreate={canCreate}
          />
        )}

        {/* Mobile cards */}
        {isMobile && (
          <Box sx={{ p: 2 }}>
            {isLoading
              ? Array.from({ length: 5 }, (_, i) => (
                  <Box key={i} sx={{ mb: 1.5, height: 90, bgcolor: 'action.hover', borderRadius: 2 }} />
                ))
              : leaves.map((l) => (
                  <LeaveCard
                    key={l.id}
                    leave={l}
                    onClick={() => handleView(l)}
                    onMenuOpen={(e, leave) => handleEdit(leave)}
                  />
                ))}
          </Box>
        )}

        <LeavePagination
          page={page} pageSize={pageSize} totalElements={totalElements}
          onPageChange={setPage}
          onPageSizeChange={(s) => { setPageSize(s); setPage(0); }}
          disabled={isLoading || isFetching}
        />
      </Card>

      {/* Dialogs */}
      <LeaveDialog
        open={dialogMode !== null}
        mode={dialogMode ?? 'create'}
        defaultValues={editLeave ?? undefined}
        isSubmitting={createMutation.isPending || updateMutation.isPending}
        serverErrors={serverErrors}
        onSubmit={handleSubmit}
        onClose={handleClose}
      />

      <LeaveApprovalDialog
        open={Boolean(approveTarget)}
        leave={approveTarget}
        isApproving={approveMutation.isPending}
        onConfirm={handleApprove}
        onCancel={() => setApproveTarget(null)}
      />

      <RejectLeaveDialog
        open={Boolean(rejectTarget)}
        leave={rejectTarget}
        isRejecting={rejectMutation.isPending}
        onConfirm={handleReject}
        onCancel={() => setRejectTarget(null)}
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
