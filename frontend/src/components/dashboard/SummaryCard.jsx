/**
 * @fileoverview SummaryCard — KPI metric card with trend indicator.
 *
 * Displays a single numeric metric (e.g., total employees) alongside an
 * icon, optional trend delta, and a coloured accent. Used by
 * {@link StatisticsCards} to render the four top-level KPI tiles.
 */

import React from 'react';
import { Box, Card, CardContent, Skeleton, Typography } from '@mui/material';
import TrendingUpIcon   from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import TrendingFlatIcon from '@mui/icons-material/TrendingFlat';
import { formatCompactNumber, formatTrend, trendColor } from '@/utils/dashboardFormatters';

/**
 * Returns the appropriate trend icon component based on the change value.
 *
 * @param {number} change
 * @returns {JSX.Element}
 */
function TrendIcon({ change }) {
  if (change > 0) return <TrendingUpIcon sx={{ fontSize: 14 }} />;
  if (change < 0) return <TrendingDownIcon sx={{ fontSize: 14 }} />;
  return <TrendingFlatIcon sx={{ fontSize: 14 }} />;
}

/**
 * @typedef {Object} SummaryCardProps
 * @property {string}        label       - Card label (e.g., "Total Employees").
 * @property {number}        value       - The primary metric value.
 * @property {React.ElementType} Icon    - MUI icon component (not an element).
 * @property {string}        iconBg      - Icon container background colour.
 * @property {string}        iconColor   - Icon fill colour.
 * @property {number}        [trend]     - Month-over-month change (positive / negative).
 * @property {string}        [trendLabel]- Short label describing the trend period.
 * @property {boolean}       [loading]   - When true renders a Skeleton placeholder.
 */

/**
 * Single KPI summary card.
 *
 * @param {SummaryCardProps} props
 * @returns {JSX.Element}
 */
export default function SummaryCard({
  label,
  value,
  Icon,
  iconBg,
  iconColor,
  trend,
  trendLabel = 'this month',
  loading = false,
}) {
  if (loading) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Skeleton variant="rectangular" width={48} height={48} sx={{ borderRadius: 2 }} />
            <Skeleton variant="text" width={60} />
          </Box>
          <Skeleton variant="text" width="40%" height={40} />
          <Skeleton variant="text" width="60%" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ height: '100%', transition: 'box-shadow 0.2s', '&:hover': { boxShadow: 4 } }}>
      <CardContent sx={{ p: 3, '&:last-child': { pb: 3 } }}>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 2 }}>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: 2,
              bgcolor: iconBg,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
            aria-hidden="true"
          >
            <Icon sx={{ color: iconColor, fontSize: 24 }} />
          </Box>

          {trend != null && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 0.25,
                color: trendColor(trend),
              }}
              aria-label={`Trend: ${formatTrend(trend)} ${trendLabel}`}
            >
              <TrendIcon change={trend} />
              <Typography variant="caption" fontWeight={600}>
                {formatTrend(trend)}
              </Typography>
            </Box>
          )}
        </Box>

        <Typography
          variant="h4"
          fontWeight={700}
          sx={{ lineHeight: 1.2, mb: 0.5 }}
          aria-label={`${label}: ${value}`}
        >
          {formatCompactNumber(value)}
        </Typography>

        <Typography variant="body2" color="text.secondary">
          {label}
        </Typography>

        {trend != null && (
          <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 0.5 }}>
            {trend >= 0 ? '+' : ''}{trend} {trendLabel}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}
