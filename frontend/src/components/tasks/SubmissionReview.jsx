/**
 * @fileoverview SubmissionReview — manager reviews an employee's task submission.
 *
 * Phase 6B:   Approve / request-changes actions.
 * Phase 6B.1: Shows attachment metadata with download action.
 *
 * Shown on the manager task detail page when the task is in SUBMITTED status.
 * Provides Approve and Request Changes actions with appropriate confirmation.
 */

import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded';
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import { downloadAttachment } from '@/services/taskSubmissionApi';

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
}

/**
 * @param {{
 *   submission: import('../../services/taskSubmissionApi').TaskSubmissionResponse,
 *   onApprove: () => Promise<void>,
 *   onRequestChanges: (reviewComment: string) => Promise<void>,
 *   isProcessing: boolean,
 * }} props
 */
export default function SubmissionReview({
  submission,
  onApprove,
  onRequestChanges,
  isProcessing = false,
}) {
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [changesDialogOpen, setChangesDialogOpen] = useState(false);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewCommentError, setReviewCommentError] = useState('');

  const formattedSubmittedAt = submission.submittedAt
    ? new Date(submission.submittedAt).toLocaleString()
    : '—';

  const handleApproveConfirm = async () => {
    setApproveDialogOpen(false);
    await onApprove();
  };

  const handleRequestChangesSubmit = async () => {
    if (!reviewComment.trim()) {
      setReviewCommentError('Please explain what changes are required');
      return;
    }
    setReviewCommentError('');
    setChangesDialogOpen(false);
    await onRequestChanges(reviewComment.trim());
    setReviewComment('');
  };

  return (
    <Card variant="outlined" sx={{ mb: 3, borderColor: 'warning.main', borderWidth: 2 }}>
      <CardContent>
        <Typography variant="subtitle1" fontWeight={700} gutterBottom color="warning.dark">
          📋 Submission Awaiting Review
        </Typography>

        <Divider sx={{ mb: 2 }} />

        {/* Submitted by / when */}
        <Stack direction="row" spacing={3} flexWrap="wrap" sx={{ mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <PersonRoundedIcon fontSize="small" color="action" />
            <Box>
              <Typography variant="caption" color="text.secondary" display="block">
                Submitted By
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {submission.submittedByName ?? '—'}
              </Typography>
            </Box>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <AccessTimeRoundedIcon fontSize="small" color="action" />
            <Box>
              <Typography variant="caption" color="text.secondary" display="block">
                Submitted At
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {formattedSubmittedAt}
              </Typography>
            </Box>
          </Box>
        </Stack>

        {/* Submission notes */}
        {submission.submissionNotes && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
              Submission Notes
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
              {submission.submissionNotes}
            </Typography>
          </Box>
        )}

        {/* Work completed */}
        {submission.workCompleted && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
              Work Completed
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
              {submission.workCompleted}
            </Typography>
          </Box>
        )}

        {/* Additional comments */}
        {submission.additionalComments && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
              Additional Comments
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', fontStyle: 'italic' }}>
              {submission.additionalComments}
            </Typography>
          </Box>
        )}

        {/* ── Attachment section (Phase 6B.1) ──────────────────────────── */}
        {submission.hasAttachment && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
              Attached File
            </Typography>
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                p: 1.5,
                border: '1px solid',
                borderColor: 'info.light',
                borderRadius: 1,
                bgcolor: 'info.50',
              }}
            >
              <AttachFileRoundedIcon color="info" />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="body2" fontWeight={600} noWrap>
                  📎 {submission.attachmentOriginalName || 'attachment'}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {submission.attachmentMimeType && (
                    <>{submission.attachmentMimeType.split('/')[1]?.toUpperCase()}</>
                  )}
                  {submission.attachmentSizeBytes != null && (
                    <> · {formatBytes(submission.attachmentSizeBytes)}</>
                  )}
                </Typography>
              </Box>
              <Button
                size="small"
                variant="outlined"
                color="info"
                startIcon={<DownloadRoundedIcon />}
                onClick={async () => {
                  try {
                    await downloadAttachment(
                      submission.id,
                      submission.attachmentOriginalName || 'attachment',
                    );
                  } catch {
                    /* browser will show error */
                  }
                }}
              >
                Download
              </Button>
            </Box>
          </Box>
        )}

        <Divider sx={{ mb: 2 }} />

        {/* Action buttons */}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <Button
            variant="contained"
            color="success"
            startIcon={
              isProcessing ? (
                <CircularProgress size={16} color="inherit" />
              ) : (
                <CheckCircleRoundedIcon />
              )
            }
            disabled={isProcessing}
            onClick={() => setApproveDialogOpen(true)}
          >
            Approve Submission
          </Button>
          <Button
            variant="outlined"
            color="warning"
            startIcon={<EditRoundedIcon />}
            disabled={isProcessing}
            onClick={() => setChangesDialogOpen(true)}
          >
            Request Changes
          </Button>
        </Stack>
      </CardContent>

      {/* Approve confirmation dialog */}
      <Dialog open={approveDialogOpen} onClose={() => setApproveDialogOpen(false)}>
        <DialogTitle>Approve Submission?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            This will mark the task as <strong>Completed</strong> and notify the employee. This
            action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleApproveConfirm} variant="contained" color="success" autoFocus>
            Approve
          </Button>
        </DialogActions>
      </Dialog>

      {/* Request changes dialog */}
      <Dialog
        open={changesDialogOpen}
        onClose={() => setChangesDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Request Changes</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Explain what changes are needed. The employee will be notified and the task will revert
            to <strong>In Progress</strong>.
          </DialogContentText>
          <TextField
            label="Review Comment"
            placeholder="Describe what needs to be changed..."
            multiline
            minRows={4}
            fullWidth
            autoFocus
            value={reviewComment}
            onChange={(e) => {
              setReviewComment(e.target.value);
              if (reviewCommentError) setReviewCommentError('');
            }}
            error={Boolean(reviewCommentError)}
            helperText={reviewCommentError}
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setChangesDialogOpen(false);
              setReviewComment('');
              setReviewCommentError('');
            }}
          >
            Cancel
          </Button>
          <Button onClick={handleRequestChangesSubmit} variant="contained" color="warning">
            Request Changes
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
}
