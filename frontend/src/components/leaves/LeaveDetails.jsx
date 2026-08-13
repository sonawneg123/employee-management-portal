/**
 * @fileoverview LeaveDetails — full read-only detail view for a single leave request.
 */

import React from 'react';
import { Box, Card, CardContent, Chip, Divider, Grid, Skeleton, Typography } from '@mui/material';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import PersonIcon from '@mui/icons-material/Person';
import ApartmentIcon from '@mui/icons-material/Apartment';
import NotesIcon from '@mui/icons-material/Notes';
import AssignmentIcon from '@mui/icons-material/Assignment';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import LinkIcon from '@mui/icons-material/Link';
import LeaveStatusChip from './LeaveStatusChip';
import LeaveTypeChip from './LeaveTypeChip';
import { formatLeaveDateRange, formatLeaveWorkingDays } from '@/utils/leaveFormatters';
import { formatDate, formatDateTime } from '@/utils/dateUtils';

/**
 * @param {{ icon: React.ReactNode, label: string, value: React.ReactNode }} props
 */
function DetailRow({ icon, label, value }) {
  return (
    <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', py: 1 }}>
      <Box sx={{ color: 'text.secondary', mt: 0.3, flexShrink: 0 }}>{icon}</Box>
      <Box>
        <Typography variant="caption" color="text.secondary" display="block">
          {label}
        </Typography>
        <Typography variant="body2" fontWeight={500}>
          {value}
        </Typography>
      </Box>
    </Box>
  );
}

/**
 * @typedef {Object} LeaveDetailsProps
 * @property {import('@/services/leaveApi').LeaveRequestResponse | undefined} leave
 * @property {boolean} isLoading
 */

/**
 * Full read-only leave request detail card.
 *
 * @param {LeaveDetailsProps} props
 * @returns {JSX.Element}
 */
export default function LeaveDetails({ leave, isLoading }) {
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            {[0, 1].map((i) => (
              <Skeleton
                key={i}
                variant="rectangular"
                width={100}
                height={28}
                sx={{ borderRadius: 4 }}
              />
            ))}
          </Box>
          {[0, 1, 2, 3, 4].map((i) => (
            <Box key={i} sx={{ display: 'flex', gap: 2, mb: 1.5 }}>
              <Skeleton variant="circular" width={24} height={24} />
              <Box sx={{ flex: 1 }}>
                <Skeleton variant="text" width="30%" />
                <Skeleton variant="text" width="60%" />
              </Box>
            </Box>
          ))}
        </CardContent>
      </Card>
    );
  }

  if (!leave) return null;

  return (
    <Card>
      <CardContent>
        {/* Header chips */}
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
          <LeaveTypeChip type={leave.leaveType} />
          <LeaveStatusChip status={leave.status} />
          {leave.isEmergency && (
            <Chip
              icon={<WarningAmberIcon />}
              label="Emergency"
              color="error"
              size="small"
              aria-label="Emergency leave"
            />
          )}
        </Box>

        <Divider sx={{ mb: 1.5 }} />

        <Grid container spacing={0}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailRow
              icon={<PersonIcon fontSize="small" />}
              label="Employee"
              value={leave.employeeName ?? '—'}
            />
            <DetailRow
              icon={<ApartmentIcon fontSize="small" />}
              label="Department"
              value={leave.departmentName ?? '—'}
            />
            <DetailRow
              icon={<CalendarTodayIcon fontSize="small" />}
              label="Leave Period"
              value={`${formatLeaveDateRange(leave.startDate, leave.endDate)} · ${formatLeaveWorkingDays(leave.startDate, leave.endDate)}`}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailRow
              icon={<NotesIcon fontSize="small" />}
              label="Reason"
              value={leave.reason || '—'}
            />
            {leave.attachmentUrl && (
              <DetailRow
                icon={<LinkIcon fontSize="small" />}
                label="Attachment"
                value={
                  <a href={leave.attachmentUrl} target="_blank" rel="noopener noreferrer">
                    View document
                  </a>
                }
              />
            )}
            <DetailRow
              icon={<AssignmentIcon fontSize="small" />}
              label="Submitted"
              value={formatDateTime(leave.createdAt)}
            />
            {leave.reviewedAt && (
              <DetailRow
                icon={<AssignmentIcon fontSize="small" />}
                label="Reviewed"
                value={`${formatDate(leave.reviewedAt)}${leave.reviewedBy ? ` by ${leave.reviewedBy}` : ''}`}
              />
            )}
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}
