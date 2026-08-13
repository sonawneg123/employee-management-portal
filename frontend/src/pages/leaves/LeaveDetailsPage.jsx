/**
 * @fileoverview LeaveDetailsPage — full detail view for a single leave request.
 *
 * Fetches the leave by UUID from the route param and renders:
 * - {@link LeaveDetails}         — full read-only detail card
 * - Approve / Reject / Cancel action buttons (role-gated)
 *
 * Status transitions:
 *   PENDING → APPROVED (HR/Admin/Manager)
 *   PENDING → REJECTED (HR/Admin/Manager)
 *   PENDING → CANCELLED (Employee own)
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Box, Button, Snackbar, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';

import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';
import { ROUTES } from '@/constants/routes';
import {
  useLeave,
  useUpdateLeave,
  useDeleteLeave,
  useApproveLeave,
  useRejectLeave,
} from '@/hooks/useLeaveHooks';

import LeaveDetails from '@/components/leaves/LeaveDetails';
import LeaveDialog from '@/components/leaves/LeaveDialog';
import LeaveApprovalDialog from '@/components/leaves/LeaveApprovalDialog';
import RejectLeaveDialog from '@/components/leaves/RejectLeaveDialog';

/**
 * Individual leave request detail page.
 *
 * @returns {JSX.Element}
 */
export default function LeaveDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasAnyRole } = useAuth();

  const canApprove = hasAnyRole([ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]);
  const canEdit = hasAnyRole([ROLES.ADMIN, ROLES.HR]);

  const { data: leave, isLoading, isError, error } = useLeave(id);

  const updateMutation = useUpdateLeave();
  const deleteMutation = useDeleteLeave();
  const approveMutation = useApproveLeave();
  const rejectMutation = useRejectLeave();

  const [editOpen, setEditOpen] = useState(false);
  const [approveOpen, setApproveOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });

  const showSnackbar = useCallback(
    (severity, message) => setSnackbar({ open: true, severity, message }),
    [],
  );
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  const handleEditSubmit = useCallback(
    async (payload) => {
      try {
        await updateMutation.mutateAsync({ id, payload });
        showSnackbar('success', 'Leave request updated.');
        setEditOpen(false);
      } catch (err) {
        if (!err?.violations) showSnackbar('error', err?.message ?? 'Failed to update.');
      }
    },
    [id, updateMutation, showSnackbar],
  );

  const handleApprove = useCallback(async () => {
    try {
      await approveMutation.mutateAsync(id);
      showSnackbar('success', 'Leave approved.');
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to approve.');
    } finally {
      setApproveOpen(false);
    }
  }, [id, approveMutation, showSnackbar]);

  const handleReject = useCallback(
    async (reason) => {
      try {
        await rejectMutation.mutateAsync({ id, reason });
        showSnackbar('success', 'Leave rejected.');
      } catch (err) {
        showSnackbar('error', err?.message ?? 'Failed to reject.');
      } finally {
        setRejectOpen(false);
      }
    },
    [id, rejectMutation, showSnackbar],
  );

  const handleCancel = useCallback(async () => {
    try {
      await deleteMutation.mutateAsync(id);
      showSnackbar('success', 'Leave request cancelled.');
      navigate(ROUTES.LEAVES, { replace: true });
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to cancel.');
    }
  }, [id, deleteMutation, navigate, showSnackbar]);

  const isPending = leave?.status === 'PENDING';

  return (
    <>
      <Helmet>
        <title>Leave Details — PeopleCore HR</title>
      </Helmet>

      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          mb: 4,
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate(ROUTES.LEAVES)}
            variant="outlined"
            size="small"
            aria-label="Back"
            sx={{ borderRadius: '8px', fontWeight: 600 }}
          >
            Back
          </Button>
          <Typography variant="h2" fontWeight={800} sx={{ letterSpacing: '-0.02em' }}>
            {isLoading ? 'Loading…' : 'Leave Request'}
          </Typography>
        </Box>

        {!isLoading && !isError && leave && (
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {canEdit && isPending && (
              <Button
                variant="outlined"
                startIcon={<EditIcon />}
                onClick={() => setEditOpen(true)}
                aria-label="Edit"
                sx={{ borderRadius: '8px', fontWeight: 600 }}
              >
                Edit
              </Button>
            )}
            {canApprove && isPending && (
              <Button
                variant="contained"
                color="success"
                startIcon={<CheckCircleIcon />}
                onClick={() => setApproveOpen(true)}
                aria-label="Approve"
                sx={{ borderRadius: '8px', fontWeight: 600 }}
              >
                Approve ✅
              </Button>
            )}
            {canApprove && isPending && (
              <Button
                variant="outlined"
                color="error"
                startIcon={<CancelIcon />}
                onClick={() => setRejectOpen(true)}
                aria-label="Reject"
                sx={{ borderRadius: '8px', fontWeight: 600 }}
              >
                Reject
              </Button>
            )}
            {isPending && (
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={handleCancel}
                aria-label="Cancel request"
                sx={{ borderRadius: '8px', fontWeight: 600 }}
              >
                Cancel Request
              </Button>
            )}
          </Box>
        )}
      </Box>

      {isError && (
        <Alert
          severity={error?.status === 404 ? 'warning' : 'error'}
          sx={{ mb: 3, borderRadius: '10px' }}
        >
          {error?.status === 404
            ? 'Leave request not found.'
            : (error?.message ?? 'Failed to load details.')}
        </Alert>
      )}

      <LeaveDetails leave={leave} isLoading={isLoading} />

      <LeaveDialog
        open={editOpen}
        mode="edit"
        defaultValues={leave}
        isSubmitting={updateMutation.isPending}
        serverErrors={updateMutation.error?.violations}
        onSubmit={handleEditSubmit}
        onClose={() => setEditOpen(false)}
      />

      <LeaveApprovalDialog
        open={approveOpen}
        leave={leave ?? null}
        isApproving={approveMutation.isPending}
        onConfirm={handleApprove}
        onCancel={() => setApproveOpen(false)}
      />

      <RejectLeaveDialog
        open={rejectOpen}
        leave={leave ?? null}
        isRejecting={rejectMutation.isPending}
        onConfirm={handleReject}
        onCancel={() => setRejectOpen(false)}
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
