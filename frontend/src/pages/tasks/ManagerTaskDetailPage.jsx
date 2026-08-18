/**
 * @fileoverview ManagerTaskDetailPage — task detail view for managers/HR.
 *
 * Phase 6A: View + edit tasks.
 * Phase 6B: Submission review (approve / request changes).
 * Phase 6C-6E: Activity timeline, comments, attachments, reassign dialog.
 * Phase 7B: AI Evaluation section.
 * Phase 7D: AI score trend and task insights.
 */

import React, { useCallback, useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  Snackbar,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import SyncAltRoundedIcon from '@mui/icons-material/SyncAltRounded';

import { ROUTES } from '@/constants/routes';
import {
  useTask,
  useUpdateTask,
  useReassignTask,
  useEmployeeAvailability,
} from '@/hooks/useTaskHooks';
import {
  useApproveSubmission,
  useLatestSubmission,
  useRequestChanges,
} from '@/hooks/useTaskSubmissionHooks';
import TaskDetailView from '@/components/tasks/TaskDetailView';
import TaskForm from '@/components/tasks/TaskForm';
import SubmissionReview from '@/components/tasks/SubmissionReview';
import TaskActivityTimeline from '@/components/tasks/TaskActivityTimeline';
import TaskComments from '@/components/tasks/TaskComments';
import TaskAttachments from '@/components/tasks/TaskAttachments';
import EmployeeAvailabilitySelector from '@/components/tasks/EmployeeAvailabilitySelector';
import TaskAiEvaluationSection from '@/components/tasks/TaskAiEvaluationSection';
import ManagerAiTrendSection from '@/components/tasks/ManagerAiTrendSection';

/**
 * Manager task detail page.
 *
 * @returns {JSX.Element}
 */
export default function ManagerTaskDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const { data: task, isLoading, isError } = useTask(id);

  const [editOpen, setEditOpen] = useState(false);
  const [formValues, setFormValues] = useState({});
  const [formErrors, setFormErrors] = useState({});
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });
  const showSnackbar = useCallback(
    (severity, message) => setSnackbar({ open: true, severity, message }),
    [],
  );
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  // ── Reassign dialog ────────────────────────────────────────────────────────
  const [reassignOpen, setReassignOpen] = useState(false);
  const [reassignEmployeeId, setReassignEmployeeId] = useState('');
  const [reassignReason, setReassignReason] = useState('');

  const updateMutation = useUpdateTask();
  const reassignMutation = useReassignTask();

  // Submission review hooks
  const approveMutation = useApproveSubmission();
  const requestChangesMutation = useRequestChanges();

  const needsSubmission = task && ['SUBMITTED', 'COMPLETED'].includes(task.status);
  const { data: latestSubmission, isLoading: isLoadingSubmission } = useLatestSubmission(id, {
    enabled: Boolean(id) && needsSubmission,
  });

  // AI evaluation is available whenever a submission exists (any status that has a submission)
  const hasAnySubmission = task && ['SUBMITTED', 'COMPLETED', 'IN_PROGRESS'].includes(task.status);
  const { data: latestSubmissionForAi } = useLatestSubmission(id, {
    enabled: Boolean(id) && hasAnySubmission && !needsSubmission,
  });
  // Use whichever submission data is available
  const submissionForAi = latestSubmission ?? latestSubmissionForAi ?? null;

  // Availability for the reassign + edit dialogs
  const { data: availabilityEmployees = [] } = useEmployeeAvailability();

  // Seed form when task loads
  useEffect(() => {
    if (task) {
      setFormValues({
        title: task.title ?? '',
        description: task.description ?? '',
        guidelines: task.guidelines ?? '',
        acceptanceCriteria: task.acceptanceCriteria ?? '',
        assignedEmployeeId: task.assignedEmployeeId ?? '',
        priority: task.priority ?? 'MEDIUM',
        status: task.status ?? 'DRAFT',
        dueDate: task.dueDate ?? '',
        estimatedHours: task.estimatedHours ?? '',
        category: task.category ?? '',
      });
    }
  }, [task]);

  const handleFormChange = useCallback(
    (field, value) => {
      setFormValues((prev) => ({ ...prev, [field]: value }));
      if (formErrors[field]) setFormErrors((e) => ({ ...e, [field]: undefined }));
    },
    [formErrors],
  );

  const validateForm = () => {
    const errs = {};
    if (!formValues.title?.trim()) errs.title = 'Title is required';
    if (!formValues.dueDate) errs.dueDate = 'Due date is required';
    return errs;
  };

  const handleUpdate = async () => {
    const errs = validateForm();
    if (Object.keys(errs).length > 0) { setFormErrors(errs); return; }

    const payload = {
      title: formValues.title.trim(),
      description: formValues.description || null,
      guidelines: formValues.guidelines || null,
      acceptanceCriteria: formValues.acceptanceCriteria || null,
      assignedEmployeeId: formValues.assignedEmployeeId || null,
      priority: formValues.priority || 'MEDIUM',
      status: formValues.status || task.status,
      dueDate: formValues.dueDate,
      estimatedHours: formValues.estimatedHours ? Number(formValues.estimatedHours) : null,
      category: formValues.category || null,
    };

    try {
      await updateMutation.mutateAsync({ id, payload });
      setEditOpen(false);
      showSnackbar('success', 'Task updated successfully.');
    } catch (err) {
      const detail = err?.response?.data?.detail ?? 'Failed to update task.';
      showSnackbar('error', detail);
    }
  };

  // ── Submission review ──────────────────────────────────────────────────────
  const handleApprove = async () => {
    if (!latestSubmission) return;
    try {
      await approveMutation.mutateAsync(latestSubmission.id);
      showSnackbar('success', 'Submission approved! Task marked as Completed.');
    } catch (err) {
      showSnackbar('error', err?.response?.data?.detail ?? 'Failed to approve submission.');
    }
  };

  const handleRequestChanges = async (reviewComment) => {
    if (!latestSubmission) return;
    try {
      await requestChangesMutation.mutateAsync({
        submissionId: latestSubmission.id,
        payload: { reviewComment },
      });
      showSnackbar('success', 'Changes requested. Employee has been notified.');
    } catch (err) {
      showSnackbar('error', err?.response?.data?.detail ?? 'Failed to request changes.');
    }
  };

  // ── Reassign ───────────────────────────────────────────────────────────────
  const handleReassign = async () => {
    if (!reassignEmployeeId) {
      showSnackbar('warning', 'Please select an employee to reassign to.');
      return;
    }
    try {
      await reassignMutation.mutateAsync({
        taskId: id,
        newEmployeeId: reassignEmployeeId,
        reason: reassignReason || undefined,
      });
      setReassignOpen(false);
      setReassignEmployeeId('');
      setReassignReason('');
      showSnackbar('success', 'Task reassigned successfully.');
    } catch (err) {
      showSnackbar('error', err?.response?.data?.detail ?? 'Failed to reassign task.');
    }
  };

  const isReviewProcessing = approveMutation.isPending || requestChangesMutation.isPending;

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
        <Alert severity="error">Task not found or you do not have access.</Alert>
        <Button startIcon={<ArrowBackRoundedIcon />} onClick={() => navigate(ROUTES.MANAGER_TASKS)} sx={{ mt: 2 }}>
          Back to Tasks
        </Button>
      </Box>
    );
  }

  return (
    <>
      <Helmet><title>{task.title} | Task Management</title></Helmet>

      <Box sx={{ p: { xs: 2, md: 3 } }}>
        {/* Header navigation */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          <IconButton onClick={() => navigate(ROUTES.MANAGER_TASKS)}>
            <ArrowBackRoundedIcon />
          </IconButton>
          <Typography variant="h6" color="text.secondary">Task Management</Typography>
          <Typography variant="h6" color="text.secondary">›</Typography>
          <Typography variant="h6" noWrap sx={{ maxWidth: 400 }}>{task.title}</Typography>
          <Box sx={{ ml: 'auto', display: 'flex', gap: 1 }}>
            <Tooltip title="Reassign task to another employee">
              <Button
                variant="outlined"
                startIcon={<SyncAltRoundedIcon />}
                onClick={() => setReassignOpen(true)}
                size="small"
              >
                Reassign
              </Button>
            </Tooltip>
            <Button
              variant="outlined"
              startIcon={<EditRoundedIcon />}
              onClick={() => setEditOpen(true)}
            >
              Edit Task
            </Button>
          </Box>
        </Box>

        <TaskDetailView task={task} />

        {/* ── Submission review ─────────────────────────────────────────── */}
        {task.status === 'SUBMITTED' && (
          <Box sx={{ mt: 3 }}>
            {isLoadingSubmission ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress size={28} />
              </Box>
            ) : latestSubmission ? (
              <SubmissionReview
                submission={latestSubmission}
                onApprove={handleApprove}
                onRequestChanges={handleRequestChanges}
                isProcessing={isReviewProcessing}
              />
            ) : (
              <Alert severity="warning">
                This task is marked as SUBMITTED but no submission record was found.
              </Alert>
            )}
          </Box>
        )}

        {task.status === 'COMPLETED' && latestSubmission && (
          <Box sx={{ mt: 3 }}>
            <Alert severity="success">
              <Typography variant="body2" fontWeight={600}>Task Completed</Typography>
              <Typography variant="body2">
                Approved by {latestSubmission.reviewedByName ?? 'Manager'}
                {latestSubmission.reviewedAt
                  ? ` on ${new Date(latestSubmission.reviewedAt).toLocaleString()}`
                  : ''}.
              </Typography>
            </Alert>
          </Box>
        )}

        {/* ── AI Evaluation section (Phase 7B) ──────────────────────── */}
        <Box sx={{ mt: 3 }}>
          <TaskAiEvaluationSection
            taskId={id}
            submission={submissionForAi}
            onRunComplete={(review) => {
              showSnackbar(
                review.status === 'COMPLETED' ? 'success' : 'info',
                review.status === 'COMPLETED'
                  ? 'AI evaluation completed successfully.'
                  : 'AI evaluation has been queued.',
              );
            }}
          />
        </Box>

        {/* ── AI Score Trend & Insights (Phase 7D) — manager-only ───── */}
        <ManagerAiTrendSection taskId={id} />

        {/* ── Attachments, Comments, Timeline ──────────────────────────── */}
        <Grid container spacing={3} sx={{ mt: 1 }}>
          <Grid size={{ xs: 12, md: 6 }}>
            <TaskAttachments taskId={id} isManager />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TaskActivityTimeline taskId={id} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TaskComments taskId={id} />
          </Grid>
        </Grid>
      </Box>

      {/* ── Edit dialog ───────────────────────────────────────────────────── */}
      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Edit Task</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <TaskForm
            values={formValues}
            errors={formErrors}
            onChange={handleFormChange}
            onSubmit={handleUpdate}
            onCancel={() => setEditOpen(false)}
            employees={availabilityEmployees}
            showAvailability
            isSubmitting={updateMutation.isPending}
            submitLabel="Save Changes"
          />
        </DialogContent>
      </Dialog>

      {/* ── Reassign dialog ───────────────────────────────────────────────── */}
      <Dialog open={reassignOpen} onClose={() => setReassignOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reassign Task</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Currently assigned to: <strong>{task.assignedEmployeeName ?? 'Unassigned'}</strong>
          </Typography>
          <EmployeeAvailabilitySelector
            employees={availabilityEmployees}
            value={reassignEmployeeId}
            onChange={setReassignEmployeeId}
            label="Reassign To"
            currentAssigneeId={task.assignedEmployeeId ?? undefined}
          />
          <TextField
            label="Reason (optional)"
            value={reassignReason}
            onChange={(e) => setReassignReason(e.target.value)}
            fullWidth
            multiline
            rows={2}
            sx={{ mt: 2 }}
            inputProps={{ maxLength: 500 }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setReassignOpen(false)} variant="outlined">Cancel</Button>
          <Button
            onClick={handleReassign}
            variant="contained"
            disabled={reassignMutation.isPending || !reassignEmployeeId}
            startIcon={reassignMutation.isPending ? <CircularProgress size={16} /> : null}
          >
            Reassign
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── Snackbar ──────────────────────────────────────────────────────── */}
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
