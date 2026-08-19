/**
 * @fileoverview UpcomingLeavesWidget — leave KPI counts from the dashboard summary.
 *
 * Shows the number of employees on approved leave today and the number of
 * pending leave requests awaiting review.
 * Premium SaaS design — navy + gold accent.
 */

import React from 'react';
import { Box, Skeleton, Typography, useTheme } from '@mui/material';
import EventNoteRoundedIcon from '@mui/icons-material/EventNoteRounded';
import HourglassEmptyRoundedIcon from '@mui/icons-material/HourglassEmptyRounded';
import { useDashboardSummary } from '@/hooks/useDashboard';
import SectionCard from './SectionCard';

/**
 * Leave summary widget — on-leave-today count and pending-leaves count.
 *
 * @returns {JSX.Element}
 */
export default function UpcomingLeavesWidget() {
  const { data: summary, isLoading, isFetching, refresh } = useDashboardSummary();
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  /** @type {{ label: string, value: number | undefined, Icon: React.ElementType, color: string, bg: string }[]} */
  const stats = [
    {
      label: 'On Leave Today',
      value: summary?.onLeaveToday,
      Icon: EventNoteRoundedIcon,
      color: '#3B82F6',
      bg: 'rgba(59,130,246,0.1)',
    },
    {
      label: 'Pending Requests',
      value: summary?.pendingLeaves,
      Icon: HourglassEmptyRoundedIcon,
      color: '#F59E0B',
      bg: 'rgba(245,158,11,0.1)',
    },
  ];

  return (
    <SectionCard
      title="Leave Overview"
      subtitle="Live counts from today"
      showRefresh
      onRefresh={refresh}
      isFetching={isFetching}
      sx={{ height: '100%' }}
    >
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
        {stats.map(({ label, value, Icon, color, bg }) => (
          <Box
            key={label}
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1.5,
              p: 1.5,
              borderRadius: '14px',
              bgcolor: isDark ? 'rgba(255,255,255,0.03)' : bg,
              border: `1px solid ${isDark ? 'rgba(240,237,230,0.06)' : 'rgba(26,35,66,0.06)'}`,
              transition: 'all 0.15s ease',
            }}
          >
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 42,
                height: 42,
                borderRadius: '12px',
                bgcolor: isDark ? `${color}22` : bg,
                border: `1px solid ${color}30`,
                flexShrink: 0,
              }}
            >
              <Icon sx={{ fontSize: 20, color }} />
            </Box>
            <Box sx={{ flex: 1 }}>
              <Typography
                variant="caption"
                sx={{ color: isDark ? 'rgba(240,237,230,0.5)' : '#9CA3AF', fontWeight: 500 }}
              >
                {label}
              </Typography>
              {isLoading ? (
                <Skeleton variant="text" width={40} height={28} />
              ) : (
                <Typography
                  variant="h6"
                  fontWeight={800}
                  lineHeight={1.2}
                  sx={{ color: isDark ? '#F0EDE6' : '#1A2342' }}
                >
                  {value ?? 0}
                </Typography>
              )}
            </Box>
            {/* Accent line */}
            <Box
              sx={{
                width: 3,
                height: 36,
                borderRadius: 999,
                bgcolor: color,
                opacity: 0.7,
                flexShrink: 0,
              }}
              aria-hidden="true"
            />
          </Box>
        ))}
      </Box>
    </SectionCard>
  );
}
