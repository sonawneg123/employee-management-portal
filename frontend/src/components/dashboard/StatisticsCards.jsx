/**
 * @fileoverview StatisticsCards — the four top-level KPI summary tiles.
 *
 * Reads data from the {@link useDashboardSummary} hook and renders four
 * {@link SummaryCard} tiles in a responsive grid. Each tile shows the
 * metric value, icon, colour accent, and month-over-month trend.
 */

import React from 'react';
import { Grid } from '@mui/material';
import { useDashboardSummary } from '@/hooks/useDashboard';
import SummaryCard from './SummaryCard';
import { STAT_CARD_META } from '@/constants/dashboard';

/**
 * Responsive grid of four KPI summary stat cards.
 *
 * @returns {JSX.Element}
 */
export default function StatisticsCards() {
  const { data, isLoading } = useDashboardSummary();

  const stats = [
    {
      key: 'totalEmployees',
      value: data?.totalEmployees ?? 0,
      trend: data?.trendEmployees,
    },
    {
      key: 'totalDepartments',
      value: data?.totalDepartments ?? 0,
      trend: undefined,
    },
    {
      key: 'pendingLeaves',
      value: data?.pendingLeaves ?? 0,
      trend: data?.trendLeaves,
    },
    {
      key: 'presentToday',
      value: data?.presentToday ?? 0,
      trend: data?.trendAttendance,
      trendLabel: 'vs yesterday',
    },
  ];

  return (
    <Grid container spacing={3} sx={{ mb: 3 }}>
      {stats.map(({ key, value, trend, trendLabel }, index) => {
        const meta = STAT_CARD_META[key];
        return (
          <Grid
            key={key}
            size={{ xs: 12, sm: 6, lg: 3 }}
            sx={{
              animation: `fadeUp 0.28s ease-out ${0.06 + index * 0.05}s both`,
              '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
            }}
          >
            <SummaryCard
              label={meta.label}
              value={value}
              Icon={meta.Icon}
              iconBg={meta.color}
              iconColor={meta.iconColor}
              trend={trend}
              trendLabel={trendLabel}
              loading={isLoading}
            />
          </Grid>
        );
      })}
    </Grid>
  );
}
