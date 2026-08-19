/**
 * @fileoverview TaskAttachments — reference attachments panel for a task.
 *
 * Phase 6C: New component. Managers/admins can upload and delete; employees can only download.
 */

import React, { useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded';
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import UploadFileRoundedIcon from '@mui/icons-material/UploadFileRounded';

import {
  useTaskAttachments,
  useUploadTaskAttachment,
  useDeleteTaskAttachment,
} from '@/hooks/useTaskHooks';
import { downloadTaskAttachment } from '@/services/taskApi';

/**
 * Converts byte count to a human-readable string.
 *
 * @param {number} bytes
 * @returns {string}
 */
function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * Formats an ISO date to "MMM D, YYYY".
 *
 * @param {string} iso
 * @returns {string}
 */
function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

/**
 * Reference attachment panel for a task.
 *
 * @param {{ taskId: string, isManager: boolean }} props
 * @returns {JSX.Element}
 */
export default function TaskAttachments({ taskId, isManager = false }) {
  const { data: attachments, isLoading, isError } = useTaskAttachments(taskId);
  const uploadMutation = useUploadTaskAttachment();
  const deleteMutation = useDeleteTaskAttachment();

  const fileInputRef = useRef(null);
  const [uploadError, setUploadError] = useState('');
  const [downloadingId, setDownloadingId] = useState(null);

  const handleUploadClick = () => {
    setUploadError('');
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    // Reset so the same file can be re-selected
    e.target.value = '';
    setUploadError('');
    try {
      await uploadMutation.mutateAsync({ taskId, file });
    } catch (err) {
      setUploadError(
        err?.response?.data?.detail ??
          'Upload failed. Ensure the file is PDF, CSV, DOCX, or TXT and under 10 MB.',
      );
    }
  };

  const handleDelete = async (attachmentId) => {
    if (!window.confirm('Delete this attachment? This cannot be undone.')) return;
    try {
      await deleteMutation.mutateAsync({ taskId, attachmentId });
    } catch {
      // silent — the list will not change and the user can retry
    }
  };

  const handleDownload = async (attachment) => {
    setDownloadingId(attachment.id);
    try {
      await downloadTaskAttachment(taskId, attachment.id, attachment.originalName);
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack
          direction="row"
          spacing={1}
          alignItems="center"
          justifyContent="space-between"
          sx={{ mb: 2 }}
        >
          <Stack direction="row" spacing={1} alignItems="center">
            <AttachFileRoundedIcon fontSize="small" color="action" />
            <Typography variant="subtitle1" fontWeight={600}>
              Attachments
            </Typography>
          </Stack>
          {isManager && (
            <Button
              size="small"
              variant="outlined"
              startIcon={
                uploadMutation.isPending ? (
                  <CircularProgress size={14} />
                ) : (
                  <UploadFileRoundedIcon />
                )
              }
              onClick={handleUploadClick}
              disabled={uploadMutation.isPending}
            >
              Upload
            </Button>
          )}
        </Stack>

        {/* Hidden file input */}
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.csv,.docx,.txt,application/pdf,text/csv,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain"
          style={{ display: 'none' }}
          onChange={handleFileChange}
        />

        {isManager && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
            Supported formats: PDF, CSV, DOCX, TXT · Max 10 MB
          </Typography>
        )}

        {uploadError && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setUploadError('')}>
            {uploadError}
          </Alert>
        )}

        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress size={24} />
          </Box>
        )}

        {isError && <Alert severity="error">Failed to load attachments.</Alert>}

        {!isLoading && !isError && (!attachments || attachments.length === 0) && (
          <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
            No attachments yet.
          </Typography>
        )}

        {!isLoading && !isError && attachments && attachments.length > 0 && (
          <List disablePadding>
            {attachments.map((att, idx) => (
              <ListItem
                key={att.id ?? idx}
                disableGutters
                sx={{
                  borderBottom: idx < attachments.length - 1 ? '1px solid' : 'none',
                  borderColor: 'divider',
                  py: 1,
                }}
                secondaryAction={
                  <Stack direction="row" spacing={0.5}>
                    <Tooltip title="Download">
                      <span>
                        <IconButton
                          size="small"
                          onClick={() => handleDownload(att)}
                          disabled={downloadingId === att.id}
                        >
                          {downloadingId === att.id ? (
                            <CircularProgress size={16} />
                          ) : (
                            <DownloadRoundedIcon fontSize="small" />
                          )}
                        </IconButton>
                      </span>
                    </Tooltip>
                    {isManager && (
                      <Tooltip title="Delete attachment">
                        <span>
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDelete(att.id)}
                            disabled={deleteMutation.isPending}
                          >
                            <DeleteOutlineRoundedIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    )}
                  </Stack>
                }
              >
                <ListItemText
                  primary={
                    <Typography variant="body2" fontWeight={500} noWrap sx={{ maxWidth: 300 }}>
                      {att.originalName}
                    </Typography>
                  }
                  secondary={
                    <Typography variant="caption" color="text.secondary">
                      {formatSize(att.sizeBytes)} · Uploaded {formatDate(att.uploadedAt)}
                      {att.uploadedByName ? ` by ${att.uploadedByName}` : ''}
                    </Typography>
                  }
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
}
