/**
 * @fileoverview SubmissionStatusCard — shows the current submission lifecycle status to employees.
 *
 * Phase 6B:   Shown after a task has been submitted.
 * Phase 6B.1: Shows attachment metadata and download link when present.
 *
 * Shown after a task has been submitted, displaying:
 * - PENDING_REVIEW: waiting for manager
 * - APPROVED: task completed
 * - CHANGES_REQUESTED: manager's feedback prominently shown, enables resubmit
 */

import React from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Stack,
  Typography,
} from '@mui/material';
import HourglassEmptyRoundedIcon from '@mui/icons-material/HourglassEmptyRounded';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded';
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import { downloadAttachment } from '@/services/taskSubmissionApi';

/**
 * Returns a human-readable file size string.
 * @param {number} bytes
 * @returns {string}
 */
function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
}

/**
 * @param {{
 *   submission: import('../../services/taskSubmissionApi').TaskSubmissionResponse,
 * }} props
 */
export default function SubmissionStatusCard({ submission }) {
  const formattedSubmittedAt = submission.submittedAt
    ? new Date(submission.submittedAt).toLocaleString()
    : '—';
  const formattedReviewedAt = submission.reviewedAt
    ? new Date(submission.reviewedAt).toLocaleString()
    : null;

  const statusConfig = {
    PENDING_REVIEW: {
      color: 'info',
      icon: <HourglassEmptyRoundedIcon fontSize="small" />,
      label: 'Awaiting Review',
      banner: null,
    },
    APPROVED: {
      color: 'success',
      icon: <CheckCircleRoundedIcon fontSize="small" />,
      label: 'Approved',
      banner: 'success',
    },
    CHANGES_REQUESTED: {
      color: 'warning',
      icon: <ErrorOutlineRoundedIcon fontSize="small" />,
      label: 'Changes Requested',
      banner: 'warning',
    },
  };

  const config = statusConfig[submission.reviewStatus] ?? statusConfig.PENDING_REVIEW;

  const handleDownload = async () => {
    try {
      await downloadAttachment(submission.id, submission.attachmentOriginalName || 'attachment');
    } catch {
      // Silently fail — user will see the browser error
    }
  };

  return (
    <Card variant="outlined" sx={{ mb: 3 }}>
      <CardContent>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1.5 }}>
          <Typography variant="subtitle1" fontWeight={700}>
            Submission Status
          </Typography>
          <Chip icon={config.icon} label={config.label} color={config.color} size="small" />
        </Stack>

        <Divider sx={{ mb: 2 }} />

        {/* Pending review banner */}
        {submission.reviewStatus === 'PENDING_REVIEW' && (
          <Alert severity="info" icon={<HourglassEmptyRoundedIcon />} sx={{ mb: 2 }}>
            Your submission is awaiting manager review. You will be notified when it is reviewed.
          </Alert>
        )}

        {/* Approved banner */}
        {submission.reviewStatus === 'APPROVED' && (
          <Alert severity="success" icon={<CheckCircleRoundedIcon />} sx={{ mb: 2 }}>
            <Typography variant="body2" fontWeight={600}>
              Your work has been approved!
            </Typography>
            <Typography variant="body2">
              This task is now complete.
              {submission.reviewedByName && ` Reviewed by ${submission.reviewedByName}`}
              {formattedReviewedAt && ` on ${formattedReviewedAt}`}.
            </Typography>
          </Alert>
        )}

        {/* Changes requested banner */}
        {submission.reviewStatus === 'CHANGES_REQUESTED' && (
          <Alert severity="warning" icon={<ErrorOutlineRoundedIcon />} sx={{ mb: 2 }}>
            <Typography variant="body2" fontWeight={600}>
              Manager has requested changes.
            </Typography>
            {submission.reviewComment && (
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt: 0.5 }}>
                &quot;{submission.reviewComment}&quot;
              </Typography>
            )}
            {submission.reviewedByName && (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                — {submission.reviewedByName}
                {formattedReviewedAt && ` · ${formattedReviewedAt}`}
              </Typography>
            )}
          </Alert>
        )}

        {/* Submission details */}
        <Stack spacing={2}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <AccessTimeRoundedIcon fontSize="small" color="action" />
            <Box>
              <Typography variant="caption" color="text.secondary" display="block">
                Submitted
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {formattedSubmittedAt}
              </Typography>
            </Box>
          </Box>

          {submission.submissionNotes && (
            <Box>
              <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
                Your Notes
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {submission.submissionNotes}
              </Typography>
            </Box>
          )}

          {submission.workCompleted && (
            <Box>
              <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
                Work Completed
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {submission.workCompleted}
              </Typography>
            </Box>
          )}

          {submission.additionalComments && (
            <Box>
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
            <Box>
              <Divider sx={{ mb: 1.5 }} />
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
                  borderColor: 'divider',
                  borderRadius: 1,
                  bgcolor: 'action.hover',
                }}
              >
                <AttachFileRoundedIcon color="action" />
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
                  startIcon={<DownloadRoundedIcon />}
                  onClick={handleDownload}
                >
                  Download
                </Button>
              </Box>
            </Box>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}
