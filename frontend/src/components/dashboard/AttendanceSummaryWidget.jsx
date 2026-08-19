/**
 * @fileoverview AttendanceSummaryWidget — today's attendance breakdown.
 *
 * Shows present / absent / on-leave counts and the overall attendance rate
 * as a percentage alongside a simple progress bar.
 * Premium SaaS design with navy + gold accent.
 */

import React from 'react';
import { Box, LinearProgress, Skeleton, Typography, useTheme } from '@mui/material';
import { useDashboardSummary } from '@/hooks/useDashboard';
import { calcAttendanceRate, formatCompactNumber } from '@/utils/dashboardFormatters';
import SectionCard from './SectionCard';

/**
 * @typedef {Object} AttendanceRow
 * @property {string} label  - Status label.
 * @property {number} count  - Employee count.
 * @property {string} color  - Hex colour.
 * @property {string} bg     - Background for dot.
 */

/**
 * Today's attendance summary card.
 *
 * @returns {JSX.Element}
 */
export default function AttendanceSummaryWidget() {
  const { data: summary, isLoading, isFetching, refresh } = useDashboardSummary();
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const present = summary?.presentToday ?? 0;
  const total = summary?.totalEmployees ?? 0;
  const onLeave = summary?.onLeaveToday ?? 0;
  const absent = Math.max(0, total - present - onLeave);
  const rate = calcAttendanceRate(present, total);
  const rateValue = total > 0 ? (present / total) * 100 : 0;

  /** @type {AttendanceRow[]} */
  const rows = [
    { label: 'Present', count: present, color: '#10B981', bg: 'rgba(16,185,129,0.12)' },
    { label: 'Absent', count: absent, color: '#EF4444', bg: 'rgba(239,68,68,0.12)' },
    { label: 'On Leave', count: onLeave, color: '#3B82F6', bg: 'rgba(59,130,246,0.12)' },
  ];

  return (
    <SectionCard
      title="Today's Attendance"
      subtitle={isLoading ? undefined : `Attendance rate: ${rate}`}
      showRefresh
      onRefresh={refresh}
      isFetching={isFetching}
      sx={{ height: '100%' }}
    >
      {isLoading ? (
        <Box sx={{ pt: 1 }}>
          <Skeleton variant="rectangular" height={8} sx={{ borderRadius: 999, mb: 2 }} />
          {[0, 1, 2].map((i) => (
            <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
              <Skeleton variant="text" width="40%" />
              <Skeleton variant="text" width="15%" />
            </Box>
          ))}
        </Box>
      ) : (
        <Box>
          {/* Attendance rate progress bar */}
          <Box sx={{ mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography
                variant="caption"
                sx={{ color: isDark ? 'rgba(240,237,230,0.55)' : '#9CA3AF', fontWeight: 500 }}
              >
                Attendance Rate
              </Typography>
              <Typography variant="caption" fontWeight={700} sx={{ color: '#10B981' }}>
                {rate}
              </Typography>
            </Box>
            <LinearProgress
              variant="determinate"
              value={rateValue}
              sx={{
                height: 8,
                borderRadius: 999,
                bgcolor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(26,35,66,0.06)',
                '& .MuiLinearProgress-bar': {
                  background: 'linear-gradient(90deg, #10B981, #059669)',
                  borderRadius: 999,
                },
              }}
              aria-label={`Attendance rate: ${rate}`}
            />
          </Box>

          {/* Breakdown list */}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {rows.map(({ label, count, color, bg }) => (
              <Box
                key={label}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  p: 1.25,
                  borderRadius: '12px',
                  bgcolor: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(26,35,66,0.02)',
                  border: `1px solid ${isDark ? 'rgba(240,237,230,0.06)' : '#EBE6DA'}`,
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                  <Box
                    sx={{
                      width: 30,
                      height: 30,
                      borderRadius: '9px',
                      bgcolor: bg,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                    }}
                    aria-hidden="true"
                  >
                    <Box
                      sx={{
                        width: 8,
                        height: 8,
                        borderRadius: '50%',
                        bgcolor: color,
                      }}
                    />
                  </Box>
                  <Typography
                    variant="body2"
                    fontWeight={500}
                    sx={{ color: isDark ? 'rgba(240,237,230,0.8)' : '#374151' }}
                  >
                    {label}
                  </Typography>
                </Box>
                <Typography variant="body2" fontWeight={700} sx={{ color }}>
                  {formatCompactNumber(count)}
                </Typography>
              </Box>
            ))}
          </Box>

          <Typography
            variant="caption"
            sx={{
              display: 'block',
              mt: 1.5,
              color: isDark ? 'rgba(240,237,230,0.4)' : '#9CA3AF',
            }}
          >
            Out of {formatCompactNumber(total)} total employees
          </Typography>
        </Box>
      )}
    </SectionCard>
  );
}
