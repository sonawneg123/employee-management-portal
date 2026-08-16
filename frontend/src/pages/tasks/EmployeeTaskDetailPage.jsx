/**
 * @fileoverview EmployeeTaskDetailPage — task detail view for employees.
 *
 * Phase 6A.1: Shows a checkout restriction banner and handles Start Task.
 * Phase 6B:   Adds Submit Task section (IN_PROGRESS → SUBMITTED),
 *             shows submission status card (PENDING_REVIEW / CHANGES_REQUESTED / APPROVED),
 *             and allows resubmission after manager requests changes.
 * Phase 6C-6E: Activity timeline, comments panel, attachments (download only).
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Grid,
  IconButton,
  Snackbar,
  Tooltip,
  Typography,
} from '@mui/material';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import LockClockRoundedIcon from '@mui/icons-material/LockClockRounded';

import { ROUTES } from '@/constants/routes';
import { useTask, useUpdateTaskStatus } from '@/hooks/useTaskHooks';
import { useTodayAttendance } from '@/hooks/useAttendanceStatus';
import {
  useCreateSubmission,
  useLatestSubmission,
  useResubmit,
} from '@/hooks/useTaskSubmissionHooks';
import TaskDetailView from '@/components/tasks/TaskDetailView';
import SubmissionForm from '@/components/tasks/SubmissionForm';
import SubmissionStatusCard from '@/components/tasks/SubmissionStatusCard';
import TaskActivityTimeline from '@/components/tasks/TaskActivityTimeline';
import TaskComments from '@/components/tasks/TaskComments';
import TaskAttachments from '@/components/tasks/TaskAttachments';

/**
 * Employee task detail page with full Phase 6B submission lifecycle
 * and Phase 6C-6E timeline/comments/attachments.
 *
 * @returns {JSX.Element}
 */
export default function EmployeeTaskDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const { data: task, isLoading, isError } = useTask(id);
  const updateStatusMutation = useUpdateTaskStatus();
  const { isCheckedOut } = useTodayAttendance();

  // Submission hooks
  const createSubmissionMutation = useCreateSubmission();
  const resubmitMutation = useResubmit();

  // Fetch the latest submission when the task has been submitted or is completed
  const hasSubmission = task && ['SUBMITTED', 'COMPLETED', 'IN_PROGRESS'].includes(task.status);
  const {
    data: latestSubmission,
    isLoading: isLoadingSubmission,
  } = useLatestSubmission(id, {
    enabled: Boolean(id) && hasSubmission,
  });

  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });
  const showSnackbar = useCallback(
    (severity, message) => setSnackbar({ open: true, severity, message }),
    [],
  );
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  // ── Start task ──────────────────────────────────────────────────────────────
  const handleStartTask = async () => {
    if (isCheckedOut) {
      showSnackbar('warning', 'You have checked out. Check in again to start working on tasks.');
      return;
    }
    try {
      await updateStatusMutation.mutateAsync({ id, status: 'IN_PROGRESS' });
      showSnackbar('success', 'Task started! Status updated to In Progress.');
    } catch (err) {
      const detail = err?.response?.data?.detail ?? 'Failed to start task.';
      showSnackbar('error', detail);
    }
  };

  // ── Submit task ─────────────────────────────────────────────────────────────
  const handleSubmitTask = async (payload, file) => {
    try {
      await createSubmissionMutation.mutateAsync({ taskId: id, payload, file: file ?? null });
      showSnackbar('success', 'Task submitted for manager review!');
    } catch (err) {
      const detail = err?.response?.data?.detail ?? 'Failed to submit task.';
      showSnackbar('error', detail);
    }
  };

  // ── Resubmit after changes requested ──────────────────────────────────────
  const handleResubmit = async (payload, file) => {
    if (!latestSubmission) return;
    try {
      await resubmitMutation.mutateAsync({
        submissionId: latestSubmission.id,
        payload,
        file: file ?? null,
      });
      showSnackbar('success', 'Task resubmitted for review!');
    } catch (err) {
      const detail = err?.response?.data?.detail ?? 'Failed to resubmit task.';
      showSnackbar('error', detail);
    }
  };

  // ── Render helpers ──────────────────────────────────────────────────────────

  const showSubmitForm = task?.status === 'IN_PROGRESS'
    && (!latestSubmission || latestSubmission.reviewStatus === 'CHANGES_REQUESTED');

  const showStatusCard = latestSubmission
    && ['SUBMITTED', 'COMPLETED'].includes(task?.status);

  const showChangesRequestedCard = task?.status === 'IN_PROGRESS'
    && latestSubmission?.reviewStatus === 'CHANGES_REQUESTED';

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError || !task) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">Task not found or you do not have access to this task.</Alert>
        <Button
          startIcon={<ArrowBackRoundedIcon />}
          onClick={() => navigate(ROUTES.EMPLOYEE_TASKS)}
          sx={{ mt: 2 }}
        >
          Back to My Tasks
        </Button>
      </Box>
    );
  }

  return (
    <>
      <Helmet><title>{task.title} | My Tasks</title></Helmet>

      <Box sx={{ p: { xs: 2, md: 3 } }}>
        {/* Breadcrumb navigation */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          <IconButton onClick={() => navigate(ROUTES.EMPLOYEE_TASKS)}>
            <ArrowBackRoundedIcon />
          </IconButton>
          <Typography variant="h6" color="text.secondary">My Tasks</Typography>
          <Typography variant="h6" color="text.secondary">›</Typography>
          <Typography variant="h6" noWrap sx={{ maxWidth: 400 }}>{task.title}</Typography>
        </Box>

        {/* Checkout restriction banner */}
        {isCheckedOut && (
          <Alert
            severity="warning"
            icon={<LockClockRoundedIcon />}
            sx={{ mb: 3 }}
          >
            <Typography variant="body2" fontWeight={600}>
              You have checked out for today.
            </Typography>
            <Typography variant="body2">
              Task actions are unavailable until your next working session.
            </Typography>
          </Alert>
        )}

        {/* Task detail */}
        <TaskDetailView
          task={task}
          onStartTask={handleStartTask}
          isUpdatingStatus={updateStatusMutation.isPending}
          showStartButton={task.status === 'ASSIGNED'}
          startDisabled={isCheckedOut}
          startDisabledReason="You have checked out. Check in again to start working."
        />

        {/* ── Submission section ─────────────────────────────────────────── */}

        {showChangesRequestedCard && latestSubmission && (
          <SubmissionStatusCard submission={latestSubmission} />
        )}

        {showSubmitForm && (
          <Box sx={{ mt: 3 }}>
            <SubmissionForm
              taskId={id}
              existingSubmission={showChangesRequestedCard ? latestSubmission : null}
              isResubmit={showChangesRequestedCard}
              onSubmit={showChangesRequestedCard ? handleResubmit : handleSubmitTask}
              isSubmitting={
                createSubmissionMutation.isPending || resubmitMutation.isPending
              }
            />
          </Box>
        )}

        {showStatusCard && (
          <Box sx={{ mt: 3 }}>
            <SubmissionStatusCard submission={latestSubmission} />
          </Box>
        )}

        {/* ── Attachments, Comments, Timeline ──────────────────────────── */}
        <Grid container spacing={3} sx={{ mt: 1 }}>
          <Grid size={{ xs: 12, md: 6 }}>
            {/* isManager=false — employees can only download, not upload/delete */}
            <TaskAttachments taskId={id} isManager={false} />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TaskActivityTimeline taskId={id} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TaskComments taskId={id} />
          </Grid>
        </Grid>
      </Box>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={closeSnackbar} variant="filled">
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
