/**
 * @fileoverview TaskComments — discussion panel for a task.
 *
 * Phase 6C: New component. Shows existing comments and allows posting new ones.
 * Auto-scrolls to the newest comment after posting.
 */

import React, { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import CommentRoundedIcon from '@mui/icons-material/CommentRounded';
import SendRoundedIcon from '@mui/icons-material/SendRounded';

import { useTaskComments, useCreateTaskComment } from '@/hooks/useTaskHooks';
import EmployeeAvatar from '@/components/employees/EmployeeAvatar';

/**
 * Returns up to 2 initials from a display name.
 *
 * @param {string} name
 * @returns {string}
 */
function initials(name) {
  if (!name) return '?';
  return name
    .split(' ')
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();
}

/**
 * Formats an ISO date to a readable string.
 *
 * @param {string} iso
 * @returns {string}
 */
function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** Role → chip colour */
const ROLE_COLOR = {
  ROLE_MANAGER:  'secondary',
  ROLE_HR:       'info',
  ROLE_ADMIN:    'warning',
  ROLE_EMPLOYEE: 'default',
};

function roleLabel(role) {
  if (!role) return null;
  const map = {
    ROLE_MANAGER:  'Manager',
    ROLE_HR:       'HR',
    ROLE_ADMIN:    'Admin',
    ROLE_EMPLOYEE: 'Employee',
  };
  return map[role] ?? role;
}

/**
 * Shows the comments thread for a task and a text input to post a new comment.
 *
 * @param {{ taskId: string }} props
 * @returns {JSX.Element}
 */
export default function TaskComments({ taskId }) {
  const { data: comments, isLoading, isError } = useTaskComments(taskId);
  const createMutation = useCreateTaskComment();

  const [content, setContent] = useState('');
  const [submitError, setSubmitError] = useState('');
  const bottomRef = useRef(null);

  // Auto-scroll when comments change
  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, [comments?.length]);

  const handlePost = async () => {
    if (!content.trim()) return;
    setSubmitError('');
    try {
      await createMutation.mutateAsync({ taskId, content: content.trim() });
      setContent('');
    } catch (err) {
      setSubmitError(err?.response?.data?.detail ?? 'Failed to post comment.');
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      handlePost();
    }
  };

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
          <CommentRoundedIcon fontSize="small" color="action" />
          <Typography variant="subtitle1" fontWeight={600}>
            Discussion
          </Typography>
          {comments && (
            <Chip label={comments.length} size="small" sx={{ ml: 0.5 }} />
          )}
        </Stack>

        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress size={24} />
          </Box>
        )}

        {isError && (
          <Alert severity="error" sx={{ mb: 2 }}>Failed to load comments.</Alert>
        )}

        {!isLoading && !isError && (!comments || comments.length === 0) && (
          <Typography variant="body2" color="text.secondary" sx={{ py: 2, mb: 2 }}>
            No comments yet. Be the first to leave a note.
          </Typography>
        )}

        {!isLoading && !isError && comments && comments.length > 0 && (
          <Box sx={{ maxHeight: 380, overflowY: 'auto', mb: 2, pr: 0.5 }}>
            <Stack spacing={2}>
              {comments.map((comment, idx) => (
                <Box key={comment.id ?? idx} sx={{ display: 'flex', gap: 1.5 }}>
                  <EmployeeAvatar
                    firstName={comment.authorName?.split(' ')[0]}
                    lastName={comment.authorName?.split(' ').slice(1).join(' ')}
                    profilePhotoUrl={comment.authorId ? `/api/employees/${comment.authorId}/profile-photo` : null}
                    size={34}
                  />
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ mb: 0.5 }}>
                      <Typography variant="body2" fontWeight={600}>
                        {comment.authorName ?? 'Unknown'}
                      </Typography>
                      {comment.authorRole && (
                        <Chip
                          label={roleLabel(comment.authorRole)}
                          color={ROLE_COLOR[comment.authorRole] ?? 'default'}
                          size="small"
                          variant="outlined"
                          sx={{ height: 18, fontSize: '0.65rem' }}
                        />
                      )}
                      {comment.edited && (
                        <Tooltip title="This comment was edited">
                          <Typography variant="caption" color="text.disabled">(edited)</Typography>
                        </Tooltip>
                      )}
                      <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>
                        {formatDate(comment.createdAt)}
                      </Typography>
                    </Stack>
                    <Typography
                      variant="body2"
                      sx={{
                        bgcolor: 'action.hover',
                        borderRadius: 1,
                        px: 1.5,
                        py: 1,
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-word',
                      }}
                    >
                      {comment.content}
                    </Typography>
                  </Box>
                </Box>
              ))}
              <div ref={bottomRef} />
            </Stack>
          </Box>
        )}

        {/* Post comment */}
        {submitError && (
          <Alert severity="error" sx={{ mb: 1 }}>{submitError}</Alert>
        )}
        <Stack direction="row" spacing={1} alignItems="flex-end">
          <TextField
            value={content}
            onChange={(e) => setContent(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Write a comment… (Ctrl+Enter to post)"
            multiline
            minRows={2}
            maxRows={6}
            fullWidth
            size="small"
            inputProps={{ maxLength: 2000 }}
          />
          <Button
            variant="contained"
            endIcon={createMutation.isPending ? <CircularProgress size={14} color="inherit" /> : <SendRoundedIcon />}
            onClick={handlePost}
            disabled={!content.trim() || createMutation.isPending}
            sx={{ whiteSpace: 'nowrap', alignSelf: 'flex-end', mb: 0.5 }}
          >
            Post
          </Button>
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
          Ctrl+Enter to post quickly
        </Typography>
      </CardContent>
    </Card>
  );
}
