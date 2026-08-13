/**
 * @fileoverview LeaveTimeline — vertical timeline of an employee's leave history.
 *
 * Renders leave requests as a chronological timeline using MUI Box-based
 * layout (no MUI Lab Timeline to stay within declared package.json deps).
 */

import React from 'react';
import { Box, Card, CardContent, Divider, Skeleton, Typography } from '@mui/material';
import EventNoteIcon from '@mui/icons-material/EventNote';
import LeaveStatusChip from './LeaveStatusChip';
import LeaveTypeChip from './LeaveTypeChip';
import { formatLeaveDateRange, formatLeaveWorkingDays } from '@/utils/leaveFormatters';
import { formatDateTime } from '@/utils/dateUtils';

/**
 * @typedef {Object} LeaveTimelineProps
 * @property {import('@/services/leaveApi').LeaveRequestResponse[]} leaves
 * @property {boolean} [isLoading]
 */

/**
 * Vertical timeline of leave requests.
 *
 * @param {LeaveTimelineProps} props
 * @returns {JSX.Element}
 */
export default function LeaveTimeline({ leaves = [], isLoading = false }) {
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} gutterBottom>
            Timeline
          </Typography>
          <Divider sx={{ mb: 2 }} />
          {[0, 1, 2].map((i) => (
            <Box key={i} sx={{ display: 'flex', gap: 2, mb: 3 }}>
              <Skeleton variant="circular" width={36} height={36} />
              <Box sx={{ flex: 1 }}>
                <Skeleton variant="text" width="50%" />
                <Skeleton variant="text" width="35%" />
                <Skeleton
                  variant="rectangular"
                  width={80}
                  height={20}
                  sx={{ mt: 0.5, borderRadius: 4 }}
                />
              </Box>
            </Box>
          ))}
        </CardContent>
      </Card>
    );
  }

  if (!leaves.length) {
    return (
      <Card>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} gutterBottom>
            Timeline
          </Typography>
          <Divider sx={{ mb: 2 }} />
          <Box sx={{ py: 4, textAlign: 'center', color: 'text.disabled' }}>
            <EventNoteIcon sx={{ fontSize: 48, opacity: 0.3 }} />
            <Typography variant="body2" sx={{ mt: 1 }}>
              No leave history.
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle1" fontWeight={700} gutterBottom>
          Timeline
        </Typography>
        <Divider sx={{ mb: 2 }} />

        <Box sx={{ position: 'relative' }}>
          {/* Vertical line */}
          <Box
            sx={{
              position: 'absolute',
              left: 17,
              top: 0,
              bottom: 0,
              width: 2,
              bgcolor: 'divider',
              zIndex: 0,
            }}
          />

          {leaves.map((leave) => (
            <Box
              key={leave.id}
              sx={{ display: 'flex', gap: 2, mb: 3, position: 'relative', zIndex: 1 }}
            >
              {/* Dot */}
              <Box
                sx={{
                  width: 36,
                  height: 36,
                  borderRadius: '50%',
                  bgcolor:
                    leave.status === 'APPROVED'
                      ? 'success.main'
                      : leave.status === 'REJECTED'
                        ? 'error.main'
                        : leave.status === 'CANCELLED'
                          ? 'grey.400'
                          : 'warning.main',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                  color: '#fff',
                }}
                aria-hidden="true"
              >
                <EventNoteIcon sx={{ fontSize: 18 }} />
              </Box>

              <Box sx={{ flex: 1, pt: 0.5 }}>
                <Box
                  sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', mb: 0.5 }}
                >
                  <LeaveTypeChip type={leave.leaveType} size="small" />
                  <LeaveStatusChip status={leave.status} size="small" />
                </Box>
                <Typography variant="body2" fontWeight={500}>
                  {formatLeaveDateRange(leave.startDate, leave.endDate)}
                  {' · '}
                  <Typography component="span" variant="caption" color="text.secondary">
                    {formatLeaveWorkingDays(leave.startDate, leave.endDate)}
                  </Typography>
                </Typography>
                {leave.reason && (
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ display: 'block', mt: 0.25 }}
                  >
                    {leave.reason}
                  </Typography>
                )}
                <Typography
                  variant="caption"
                  color="text.disabled"
                  sx={{ display: 'block', mt: 0.25 }}
                >
                  Submitted {formatDateTime(leave.createdAt)}
                </Typography>
              </Box>
            </Box>
          ))}
        </Box>
      </CardContent>
    </Card>
  );
}
