/**
 * @fileoverview SubmissionForm — employee submits task work for manager review.
 *
 * Phase 6B:   Initial submission text/description fields.
 * Phase 6B.1: Adds an optional file attachment (PDF, CSV, DOCX, TXT).
 *
 * Shown when a task is IN_PROGRESS (or CHANGES_REQUESTED after revert).
 * Handles both the initial submission and resubmission flows.
 */

import React, { useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  IconButton,
  LinearProgress,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import SendRoundedIcon from '@mui/icons-material/SendRounded';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded';
import ClearRoundedIcon from '@mui/icons-material/ClearRounded';
import InsertDriveFileRoundedIcon from '@mui/icons-material/InsertDriveFileRounded';

/** Allowed MIME types mirroring the backend whitelist. */
const ALLOWED_MIME_TYPES = new Set([
  'application/pdf',
  'text/csv',
  'application/csv',
  'application/vnd.ms-excel',
  'text/plain',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/octet-stream', // some browsers send this for docx/txt
]);

/** Allowed extensions (without dot) — used as the primary guard. */
const ALLOWED_EXTENSIONS = new Set(['pdf', 'csv', 'docx', 'txt']);

/** Maximum file size in bytes — matches the backend default of 10 MB. */
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

/**
 * Extracts the lowercase extension from a filename without the leading dot.
 * @param {string} name
 * @returns {string}
 */
function getExtension(name) {
  if (!name) return '';
  const dot = name.lastIndexOf('.');
  return dot >= 0 ? name.slice(dot + 1).toLowerCase() : '';
}

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
 *   taskId: string,
 *   existingSubmission: import('../../services/taskSubmissionApi').TaskSubmissionResponse | null,
 *   isResubmit: boolean,
 *   onSubmit: (payload: { submissionNotes: string, workCompleted: string|null, additionalComments: string|null }, file: File|null) => Promise<void>,
 *   isSubmitting: boolean,
 * }} props
 */
export default function SubmissionForm({
  taskId,
  existingSubmission,
  isResubmit = false,
  onSubmit,
  isSubmitting = false,
}) {
  const [submissionNotes, setSubmissionNotes] = useState(
    isResubmit ? (existingSubmission?.submissionNotes ?? '') : '',
  );
  const [workCompleted, setWorkCompleted] = useState(
    isResubmit ? (existingSubmission?.workCompleted ?? '') : '',
  );
  const [additionalComments, setAdditionalComments] = useState(
    isResubmit ? (existingSubmission?.additionalComments ?? '') : '',
  );
  const [errors, setErrors] = useState({});

  // ── File attachment state ─────────────────────────────────────────────────
  const fileInputRef = useRef(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [fileError, setFileError] = useState('');

  const validate = () => {
    const errs = {};
    if (!submissionNotes.trim()) errs.submissionNotes = 'Submission notes are required';
    return errs;
  };

  /**
   * Validates and sets the selected file.
   * @param {File} file
   */
  const handleFileSelect = (file) => {
    if (!file) return;
    setFileError('');

    // Extension check
    const ext = getExtension(file.name);
    if (!ALLOWED_EXTENSIONS.has(ext)) {
      setFileError(
        `Unsupported file type ".${ext || 'unknown'}". Allowed types: PDF, CSV, DOCX, TXT.`,
      );
      setSelectedFile(null);
      return;
    }

    // MIME type check (best-effort — browsers may send octet-stream)
    const mime = (file.type || '').split(';')[0].trim().toLowerCase();
    if (mime && !ALLOWED_MIME_TYPES.has(mime)) {
      setFileError(
        `MIME type "${mime}" is not permitted. Please upload a PDF, CSV, DOCX, or TXT file.`,
      );
      setSelectedFile(null);
      return;
    }

    // Size check
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setFileError(
        `File is too large (${formatBytes(file.size)}). Maximum allowed size is ${formatBytes(MAX_FILE_SIZE_BYTES)}.`,
      );
      setSelectedFile(null);
      return;
    }

    setSelectedFile(file);
  };

  const handleFileInputChange = (e) => {
    const file = e.target.files?.[0] ?? null;
    handleFileSelect(file);
    // Reset input so the same file can be re-selected after removal
    e.target.value = '';
  };

  const handleFileDrop = (e) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0] ?? null;
    handleFileSelect(file);
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
    setFileError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setErrors({});
    await onSubmit(
      {
        submissionNotes: submissionNotes.trim(),
        workCompleted: workCompleted.trim() || null,
        additionalComments: additionalComments.trim() || null,
      },
      selectedFile,
    );
  };

  // ── Existing attachment info (resubmit mode) ──────────────────────────────
  const hasExistingAttachment = isResubmit && existingSubmission?.hasAttachment;

  return (
    <Card variant="outlined" sx={{ mb: 3 }}>
      <CardContent>
        <Typography variant="subtitle1" fontWeight={700} gutterBottom>
          {isResubmit ? 'Resubmit Task' : 'Submit Task for Review'}
        </Typography>

        {isResubmit && existingSubmission?.reviewComment && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            <Typography variant="body2" fontWeight={600}>
              Manager&apos;s feedback:
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt: 0.5 }}>
              {existingSubmission.reviewComment}
            </Typography>
          </Alert>
        )}

        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {isResubmit
            ? "Address the manager's feedback and resubmit your work."
            : "Describe the work you've completed so the manager can review it."}
        </Typography>

        <Box component="form" onSubmit={handleSubmit}>
          {/* ── Text fields ──────────────────────────────────────────────── */}
          <TextField
            label="Submission Notes"
            placeholder="Briefly summarise what you completed..."
            multiline
            minRows={3}
            fullWidth
            required
            value={submissionNotes}
            onChange={(e) => {
              setSubmissionNotes(e.target.value);
              if (errors.submissionNotes)
                setErrors((prev) => ({ ...prev, submissionNotes: undefined }));
            }}
            error={Boolean(errors.submissionNotes)}
            helperText={errors.submissionNotes}
            sx={{ mb: 2 }}
          />

          <TextField
            label="Work Completed"
            placeholder="List the specific work you completed..."
            multiline
            minRows={3}
            fullWidth
            value={workCompleted}
            onChange={(e) => setWorkCompleted(e.target.value)}
            sx={{ mb: 2 }}
          />

          <TextField
            label="Additional Comments"
            placeholder="Optional: known issues, caveats, follow-up items..."
            multiline
            minRows={2}
            fullWidth
            value={additionalComments}
            onChange={(e) => setAdditionalComments(e.target.value)}
            sx={{ mb: 3 }}
          />

          {/* ── File attachment section ─────────────────────────────────── */}
          <Divider sx={{ mb: 2 }} />
          <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 1 }}>
            Attach Work File
            <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
              (optional — PDF, CSV, DOCX, TXT · max 10 MB)
            </Typography>
          </Typography>

          {/* Existing attachment notice in resubmit mode */}
          {hasExistingAttachment && !selectedFile && (
            <Alert severity="info" sx={{ mb: 1.5 }} icon={<InsertDriveFileRoundedIcon />}>
              <Typography variant="body2">
                Current attachment: <strong>{existingSubmission.attachmentOriginalName}</strong>
                {existingSubmission.attachmentSizeBytes != null && (
                  <> ({formatBytes(existingSubmission.attachmentSizeBytes)})</>
                )}
                . Select a new file below to replace it, or leave empty to keep it.
              </Typography>
            </Alert>
          )}

          {/* Selected file preview */}
          {selectedFile ? (
            <Box
              sx={{
                border: '1px solid',
                borderColor: 'success.main',
                borderRadius: 1,
                p: 1.5,
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                bgcolor: 'success.50',
                mb: 1,
              }}
            >
              <InsertDriveFileRoundedIcon color="success" />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="body2" fontWeight={600} noWrap>
                  {selectedFile.name}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {getExtension(selectedFile.name).toUpperCase()} · {formatBytes(selectedFile.size)}
                </Typography>
              </Box>
              <Tooltip title="Remove file">
                <IconButton size="small" onClick={handleRemoveFile} aria-label="Remove file">
                  <ClearRoundedIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
          ) : (
            /* Drop zone */
            <Box
              onDragOver={(e) => e.preventDefault()}
              onDrop={handleFileDrop}
              onClick={() => fileInputRef.current?.click()}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') fileInputRef.current?.click();
              }}
              aria-label="File upload drop zone"
              sx={{
                border: '2px dashed',
                borderColor: fileError ? 'error.main' : 'divider',
                borderRadius: 1,
                p: 2,
                textAlign: 'center',
                cursor: 'pointer',
                mb: 1,
                '&:hover': { borderColor: 'primary.main', bgcolor: 'action.hover' },
                '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main' },
              }}
            >
              <AttachFileRoundedIcon color="action" sx={{ mb: 0.5 }} />
              <Typography variant="body2" color="text.secondary">
                Click or drag & drop a file here
              </Typography>
              <Typography variant="caption" color="text.secondary">
                PDF, CSV, DOCX, TXT — max 10 MB
              </Typography>
            </Box>
          )}

          {/* Hidden file input */}
          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf,.csv,.docx,.txt"
            style={{ display: 'none' }}
            onChange={handleFileInputChange}
            aria-label="File input"
          />

          {/* File validation error */}
          {fileError && (
            <Alert severity="error" sx={{ mb: 1 }}>
              {fileError}
            </Alert>
          )}

          {/* Upload progress (visual only while submitting with a file) */}
          {isSubmitting && selectedFile && (
            <Box sx={{ mt: 1, mb: 1 }}>
              <LinearProgress />
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ mt: 0.5, display: 'block' }}
              >
                Uploading…
              </Typography>
            </Box>
          )}

          <Divider sx={{ mb: 2, mt: 2 }} />

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
            <Button
              type="submit"
              variant="contained"
              color="primary"
              disabled={isSubmitting || Boolean(fileError)}
              startIcon={
                isSubmitting ? (
                  <CircularProgress size={16} color="inherit" />
                ) : isResubmit ? (
                  <RefreshRoundedIcon />
                ) : (
                  <SendRoundedIcon />
                )
              }
            >
              {isSubmitting
                ? 'Submitting…'
                : isResubmit
                  ? 'Resubmit for Review'
                  : 'Submit for Review'}
            </Button>

            {selectedFile && !isSubmitting && (
              <Button
                variant="outlined"
                color="inherit"
                onClick={handleRemoveFile}
                startIcon={<ClearRoundedIcon />}
              >
                Remove File
              </Button>
            )}
          </Stack>
        </Box>
      </CardContent>
    </Card>
  );
}
