/**
 * @fileoverview UpcomingLeavesWidget — displays leave KPI counts from the dashboard summary.
 *
 * Shows the number of employees on approved leave today and the number of
 * pending leave requests awaiting review. Uses the KPI data that is actually
 * returned by GET /dashboard/summary — the backend does not return individual
 * upcoming-leave records.
 */

import React from 'react';
import {
  Box,
  Divider,
  Skeleton,
  Typography,
} from '@mui/material';
import EventNoteIcon  from '@mui/icons-material/EventNote';
import PendingIcon    from '@mui/icons-material/HourglassEmpty';
import { useDashboardSummary } from '@/hooks/useDashboard';
import SectionCard from './SectionCard';

/**
 * Leave summary widget — on-leave-today count and pending-leaves count.
 *
 * @returns {JSX.Element}
 */
export default function UpcomingLeavesWidget() {
  const { data: summary, isLoading, isFetching, refresh } = useDashboardSummary();

  /** @type {{ label: string, value: number | undefined, Icon: React.ElementType }[]} */
  const stats = [
    {
      label: 'On Leave Today',
      value: summary?.onLeaveToday,
      Icon:  EventNoteIcon,
    },
    {
      label: 'Pending Requests',
      value: summary?.pendingLeaves,
      Icon:  PendingIcon,
    },
  ];

  return (
    <SectionCard
      title="Leave Overview"
      subtitle="Live counts from the dashboard summary"
      showRefresh
      onRefresh={refresh}
      isFetching={isFetching}
      sx={{ height: '100%' }}
    >
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
        {stats.map(({ label, value, Icon }, idx) => (
          <React.Fragment key={label}>
            {idx > 0 && <Divider />}
            <Box
              sx={{
                display:        'flex',
                alignItems:     'center',
                gap:            2,
                py:             2,
              }}
            >
              <Box
                sx={{
                  display:         'flex',
                  alignItems:      'center',
                  justifyContent:  'center',
                  width:           40,
                  height:          40,
                  borderRadius:    1,
                  bgcolor:         'action.hover',
                  flexShrink:      0,
                }}
              >
                <Icon sx={{ fontSize: 22, color: 'text.secondary' }} />
              </Box>
              <Box sx={{ flex: 1 }}>
                <Typography variant="body2" color="text.secondary">
                  {label}
                </Typography>
                {isLoading ? (
                  <Skeleton variant="text" width={40} height={28} />
                ) : (
                  <Typography variant="h6" fontWeight={700} lineHeight={1.2}>
                    {value ?? 0}
                  </Typography>
                )}
              </Box>
            </Box>
          </React.Fragment>
        ))}
      </Box>
    </SectionCard>
  );
}
