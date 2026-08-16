/**
 * @fileoverview CompanyPolicyForm — form for ingesting a new company policy document.
 *
 * Used by Admin and HR users to submit a document title + text content to the
 * existing Phase 2A RAG ingestion backend (POST /api/ai/rag/documents).
 *
 * Validation:
 * - title:   required, max 500 chars
 * - content: required
 * Both fields are trimmed before submission.
 *
 * States: idle | submitting | success | error
 * On success the form is reset and onSuccess() is called so the parent can
 * refresh the document list.
 */

import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  TextField,
  Typography,
} from '@mui/material';
import UploadFileRoundedIcon from '@mui/icons-material/UploadFileRounded';
import { ingestDocument } from '@/services/knowledgeApi';

const MAX_TITLE_LENGTH = 500;

/**
 * Form for adding a company policy document to the knowledge base.
 *
 * @param {{
 *   onSuccess?: (doc: import('@/services/knowledgeApi').KnowledgeDocumentResponse) => void
 * }} props
 * @returns {JSX.Element}
 */
export default function CompanyPolicyForm({ onSuccess }) {
  const [title, setTitle]           = useState('');
  const [content, setContent]       = useState('');
  const [titleTouched, setTitleTouched]     = useState(false);
  const [contentTouched, setContentTouched] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess]       = useState(false);
  const [apiError, setApiError]     = useState('');

  // ── Derived validation state ─────────────────────────────────────────────
  const titleError   = title.trim().length === 0 ? 'Title is required.' :
                       title.trim().length > MAX_TITLE_LENGTH
                         ? `Title must not exceed ${MAX_TITLE_LENGTH} characters.` : '';
  const contentError = content.trim().length === 0 ? 'Content is required.' : '';
  const canSubmit    = !submitting && !titleError && !contentError;

  // ── Handlers ──────────────────────────────────────────────────────────────

  const handleReset = () => {
    setTitle('');
    setContent('');
    setTitleTouched(false);
    setContentTouched(false);
    setSuccess(false);
    setApiError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    // Re-validate (catches the case where user submits without touching fields)
    if (!canSubmit) return;

    setSubmitting(true);
    setSuccess(false);
    setApiError('');

    try {
      const doc = await ingestDocument({
        title:   title.trim(),
        content: content.trim(),
      });
      setSuccess(true);
      setTitle('');
      setContent('');
      onSuccess?.(doc);
    } catch (err) {
      setApiError(friendlyError(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      noValidate
      aria-label="Add company policy form"
      sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
    >
      <Typography variant="h6" fontWeight={600}>
        Add Company Policy Document
      </Typography>

      {/* Success banner */}
      {success && (
        <Alert
          severity="success"
          onClose={handleReset}
          aria-label="Document submitted successfully"
        >
          Document submitted and is now being processed. It will be available to the AI
          Assistant once its status becomes&nbsp;<strong>ACTIVE</strong>.
        </Alert>
      )}

      {/* API error banner */}
      {apiError && (
        <Alert severity="error" onClose={() => setApiError('')} aria-label="Submission error">
          {apiError}
        </Alert>
      )}

      {/* Title */}
      <TextField
        label="Document Title"
        placeholder="e.g. Employee Leave Policy"
        value={title}
        onChange={(e) => { setTitle(e.target.value); setTitleTouched(true); }}
        onBlur={() => setTitleTouched(true)}
        required
        fullWidth
        disabled={submitting}
        error={titleTouched && !!titleError}
        helperText={titleTouched && titleError ? titleError : 'Enter a clear, descriptive title.'}
        inputProps={{
          'aria-label': 'Document title',
          maxLength: MAX_TITLE_LENGTH + 1,
        }}
      />

      {/* Content */}
      <TextField
        label="Document Content"
        placeholder="Paste or type the full policy text here…"
        value={content}
        onChange={(e) => { setContent(e.target.value); setContentTouched(true); }}
        onBlur={() => setContentTouched(true)}
        required
        fullWidth
        multiline
        minRows={8}
        maxRows={20}
        disabled={submitting}
        error={contentTouched && !!contentError}
        helperText={
          contentTouched && contentError
            ? contentError
            : 'Paste the full text of the policy document. Plain text only.'
        }
        inputProps={{ 'aria-label': 'Document content' }}
      />

      {/* Actions */}
      <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
        <Button
          type="submit"
          variant="contained"
          disabled={!canSubmit}
          startIcon={
            submitting ? (
              <CircularProgress size={16} color="inherit" />
            ) : (
              <UploadFileRoundedIcon />
            )
          }
          aria-label="Submit document"
        >
          {submitting ? 'Submitting…' : 'Add to Knowledge Base'}
        </Button>

        <Button
          type="button"
          variant="text"
          color="inherit"
          onClick={handleReset}
          disabled={submitting}
          aria-label="Reset form"
        >
          Clear
        </Button>
      </Box>
    </Box>
  );
}

// ── Private helpers ───────────────────────────────────────────────────────────

/**
 * Converts a normalised Axios error (or any thrown value) into a user-friendly
 * string. Never exposes stack traces or internal server details.
 *
 * @param {unknown} err
 * @returns {string}
 */
function friendlyError(err) {
  if (!err) return 'An unexpected error occurred. Please try again.';

  if (err.isNetwork || err.status === 0) {
    return 'Unable to reach the server. Please check your connection.';
  }
  if (err.status === 401 || err.status === 403) {
    return 'You do not have permission to perform this action.';
  }
  if (err.status === 400 && err.violations) {
    const msgs = Object.values(err.violations).join('; ');
    return `Validation error: ${msgs}`;
  }
  if (err.message) return err.message;
  return 'Failed to submit the document. Please try again.';
}
