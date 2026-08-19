/**
 * @fileoverview TaskActivityTimeline — shows the chronological activity log for a task.
 *
 * Phase 6C: New component. Polls via useTaskActivities every 20 seconds.
 * Activities are reversed (newest first) because the backend returns ascending order.
 */

import React from 'react';
import { Alert, Box, Card, CardContent, CircularProgress, Stack, Typography } from '@mui/material';
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded';

import { useTaskActivities } from '@/hooks/useTaskHooks';

/** Map backend eventType to a human-friendly icon label colour */
const EVENT_COLOR = {
  CREATED: '#6366f1',
  ASSIGNED: '#3b82f6',
  REASSIGNED: '#8b5cf6',
  STATUS_CHANGED: '#10b981',
  SUBMITTED: '#f59e0b',
  APPROVED: '#22c55e',
  CHANGES_REQUESTED: '#ef4444',
  REJECTED: '#ef4444',
  COMMENTED: '#64748b',
  ATTACHMENT_ADDED: '#0ea5e9',
  ATTACHMENT_DELETED: '#94a3b8',
};

/**
 * Formats an ISO timestamp to "MMM D, HH:mm".
 *
 * @param {string} iso
 * @returns {string}
 */
function formatTs(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Displays the activity timeline for a task.
 *
 * @param {{ taskId: string }} props
 * @returns {JSX.Element}
 */
export default function TaskActivityTimeline({ taskId }) {
  const { data: activities, isLoading, isError } = useTaskActivities(taskId);

  const sorted = activities ? [...activities].reverse() : [];

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
          <HistoryRoundedIcon fontSize="small" color="action" />
          <Typography variant="subtitle1" fontWeight={600}>
            Activity Timeline
          </Typography>
        </Stack>

        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress size={24} />
          </Box>
        )}

        {isError && (
          <Alert severity="error" sx={{ mt: 1 }}>
            Failed to load activity timeline.
          </Alert>
        )}

        {!isLoading && !isError && sorted.length === 0 && (
          <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
            No activity recorded yet.
          </Typography>
        )}

        {!isLoading && !isError && sorted.length > 0 && (
          <Stack spacing={0}>
            {sorted.map((activity, idx) => {
              const dotColor = EVENT_COLOR[activity.eventType] ?? '#94a3b8';
              const isLast = idx === sorted.length - 1;
              return (
                <Box
                  key={activity.id ?? idx}
                  sx={{ display: 'flex', gap: 1.5, position: 'relative' }}
                >
                  {/* Vertical line */}
                  <Box
                    sx={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      width: 20,
                      flexShrink: 0,
                    }}
                  >
                    <Box
                      sx={{
                        width: 10,
                        height: 10,
                        borderRadius: '50%',
                        bgcolor: dotColor,
                        mt: 0.5,
                        flexShrink: 0,
                      }}
                    />
                    {!isLast && <Box sx={{ width: 2, flex: 1, bgcolor: 'divider', my: 0.5 }} />}
                  </Box>

                  {/* Content */}
                  <Box sx={{ pb: isLast ? 0 : 1.5 }}>
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{ fontSize: '0.75rem', mb: 0.25 }}
                    >
                      {formatTs(activity.createdAt)}
                    </Typography>
                    <Typography variant="body2">
                      <Box component="span" fontWeight={600}>
                        {activity.actorName ?? 'System'}
                      </Box>{' '}
                      {activity.description ??
                        activity.eventType?.toLowerCase()?.replace(/_/g, ' ')}
                    </Typography>
                    {activity.fromStatus && activity.toStatus && (
                      <Typography variant="caption" color="text.secondary">
                        {activity.fromStatus.replace(/_/g, ' ')} →{' '}
                        {activity.toStatus.replace(/_/g, ' ')}
                      </Typography>
                    )}
                  </Box>
                </Box>
              );
            })}
          </Stack>
        )}
      </CardContent>
    </Card>
  );
}
