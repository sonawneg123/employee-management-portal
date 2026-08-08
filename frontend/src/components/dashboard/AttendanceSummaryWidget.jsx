/**
 * @fileoverview AttendanceSummaryWidget — today's attendance breakdown.
 *
 * Shows present / absent / on-leave counts and the overall attendance rate
 * as a percentage alongside a simple progress bar.
 */

import React from 'react';
import {
  Box,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  Skeleton,
  Typography,
} from '@mui/material';
import { useDashboardSummary } from '@/hooks/useDashboard';
import { calcAttendanceRate, formatCompactNumber } from '@/utils/dashboardFormatters';
import SectionCard from './SectionCard';

/**
 * @typedef {Object} AttendanceRow
 * @property {string} label  - Status label.
 * @property {number} count  - Employee count.
 * @property {string} color  - MUI colour token.
 */

/**
 * Today's attendance summary card.
 *
 * @returns {JSX.Element}
 */
export default function AttendanceSummaryWidget() {
  const { data: summary, isLoading, isFetching, refresh } = useDashboardSummary();

  const present   = summary?.presentToday   ?? 0;
  const total     = summary?.totalEmployees ?? 0;
  const onLeave   = summary?.onLeaveToday   ?? 0;
  const absent    = Math.max(0, total - present - onLeave);
  const rate      = calcAttendanceRate(present, total);
  const rateValue = total > 0 ? (present / total) * 100 : 0;

  /** @type {AttendanceRow[]} */
  const rows = [
    { label: 'Present',  count: present, color: 'success.main' },
    { label: 'Absent',   count: absent,  color: 'error.main'   },
    { label: 'On Leave', count: onLeave, color: 'info.main'    },
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
          <Skeleton variant="rectangular" height={8} sx={{ borderRadius: 4, mb: 2 }} />
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
          <Box sx={{ mb: 2.5 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption" color="text.secondary">
                Attendance Rate
              </Typography>
              <Typography variant="caption" fontWeight={600} color="success.main">
                {rate}
              </Typography>
            </Box>
            <LinearProgress
              variant="determinate"
              value={rateValue}
              color="success"
              sx={{ height: 8, borderRadius: 4 }}
              aria-label={`Attendance rate: ${rate}`}
            />
          </Box>

          {/* Breakdown list */}
          <List disablePadding>
            {rows.map(({ label, count, color }) => (
              <ListItem
                key={label}
                sx={{ px: 0, py: 0.5 }}
                secondaryAction={
                  <Typography variant="body2" fontWeight={700} color={color}>
                    {formatCompactNumber(count)}
                  </Typography>
                }
              >
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box
                        sx={{
                          width: 10,
                          height: 10,
                          borderRadius: '50%',
                          bgcolor: color,
                          flexShrink: 0,
                        }}
                        aria-hidden="true"
                      />
                      <Typography variant="body2">{label}</Typography>
                    </Box>
                  }
                />
              </ListItem>
            ))}
          </List>

          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.5 }}>
            Out of {formatCompactNumber(total)} total employees
          </Typography>
        </Box>
      )}
    </SectionCard>
  );
}
