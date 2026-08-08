/**
 * @fileoverview UpcomingLeavesWidget — lists leaves starting within the next 7 days.
 *
 * Extracts upcoming-leave data from the dashboard summary and renders a
 * compact list with status chips and date ranges. Visible to ADMIN, HR,
 * and MANAGER roles only; EMPLOYEE sees only their own requests (handled
 * at the RoleDashboard level).
 */

import React from 'react';
import {
  Box,
  Chip,
  List,
  ListItem,
  ListItemText,
  Skeleton,
  Typography,
} from '@mui/material';
import EventNoteIcon from '@mui/icons-material/EventNote';
import { formatDate } from '@/utils/dateUtils';
import { LEAVE_STATUS_COLORS } from '@/constants/dashboard';
import { useDashboardSummary } from '@/hooks/useDashboard';
import SectionCard from './SectionCard';

/**
 * @typedef {Object} UpcomingLeave
 * @property {string} id
 * @property {string} employeeName
 * @property {string} leaveType
 * @property {string} startDate
 * @property {string} endDate
 * @property {string} status
 */

/**
 * Upcoming leaves widget (next 7 days).
 *
 * @returns {JSX.Element}
 */
export default function UpcomingLeavesWidget() {
  const { data: summary, isLoading, isFetching, refresh } = useDashboardSummary();

  // The backend embeds upcoming leaves in the summary; fall back to empty array
  /** @type {UpcomingLeave[]} */
  const leaves = /** @type {any} */ (summary)?.upcomingLeaves ?? [];
  const isEmpty = !isLoading && leaves.length === 0;

  return (
    <SectionCard
      title="Upcoming Leaves"
      subtitle="Next 7 days"
      showRefresh
      onRefresh={refresh}
      isFetching={isFetching}
      sx={{ height: '100%' }}
    >
      {isLoading ? (
        <List disablePadding>
          {[0, 1, 2, 3].map((i) => (
            <ListItem key={i} sx={{ px: 0 }}>
              <ListItemText
                primary={<Skeleton variant="text" width="60%" />}
                secondary={<Skeleton variant="text" width="40%" />}
              />
              <Skeleton variant="rectangular" width={70} height={22} sx={{ borderRadius: 4 }} />
            </ListItem>
          ))}
        </List>
      ) : isEmpty ? (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: 160,
            gap: 1,
            color: 'text.disabled',
          }}
          role="status"
        >
          <EventNoteIcon sx={{ fontSize: 40, opacity: 0.35 }} />
          <Typography variant="body2">No upcoming leaves in the next 7 days.</Typography>
        </Box>
      ) : (
        <List disablePadding>
          {leaves.map((leave) => (
            <ListItem
              key={leave.id}
              sx={{ px: 0, py: 0.75 }}
              secondaryAction={
                <Chip
                  label={leave.status}
                  size="small"
                  sx={{
                    bgcolor: LEAVE_STATUS_COLORS[leave.status] + '20',
                    color:   LEAVE_STATUS_COLORS[leave.status],
                    fontWeight: 600,
                    fontSize: '0.6875rem',
                    borderRadius: 1,
                  }}
                />
              }
            >
              <ListItemText
                primary={
                  <Typography variant="body2" fontWeight={500} noWrap>
                    {leave.employeeName}
                  </Typography>
                }
                secondary={
                  <Typography variant="caption" color="text.secondary">
                    {leave.leaveType} · {formatDate(leave.startDate)} – {formatDate(leave.endDate)}
                  </Typography>
                }
              />
            </ListItem>
          ))}
        </List>
      )}
    </SectionCard>
  );
}
