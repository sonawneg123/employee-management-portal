/**
 * @fileoverview SummaryCard — premium KPI metric card with trend indicator.
 *
 * Displays a single numeric metric with icon, trend delta, and subtle hover animation.
 */

import React from 'react';
import { Box, Card, CardContent, Skeleton, Typography } from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import TrendingFlatIcon from '@mui/icons-material/TrendingFlat';
import { formatCompactNumber, formatTrend, trendColor } from '@/utils/dashboardFormatters';

/** @param {{ change: number }} props */
function TrendIcon({ change }) {
  if (change > 0) return <TrendingUpIcon sx={{ fontSize: 13 }} />;
  if (change < 0) return <TrendingDownIcon sx={{ fontSize: 13 }} />;
  return <TrendingFlatIcon sx={{ fontSize: 13 }} />;
}

/**
 * @typedef {Object} SummaryCardProps
 * @property {string}            label
 * @property {number}            value
 * @property {React.ElementType} Icon
 * @property {string}            iconBg
 * @property {string}            iconColor
 * @property {number}            [trend]
 * @property {string}            [trendLabel]
 * @property {boolean}           [loading]
 */

/**
 * Single KPI stat card.
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
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2.5 }}>
            <Skeleton variant="rounded" width={48} height={48} sx={{ borderRadius: '12px' }} />
            <Skeleton variant="text" width={50} height={22} />
          </Box>
          <Skeleton variant="text" width="45%" height={40} />
          <Skeleton variant="text" width="65%" height={20} />
        </CardContent>
      </Card>
    );
  }

  const trendClr = trend != null ? trendColor(trend) : 'text.secondary';

  return (
    <Card
      sx={{
        height: '100%',
        transition: 'box-shadow 0.2s ease, transform 0.2s ease',
        '&:hover': {
          boxShadow: (theme) =>
            theme.palette.mode === 'dark'
              ? '0 8px 32px rgba(0,0,0,0.5)'
              : '0 8px 32px rgba(0,0,0,0.1)',
          transform: 'translateY(-2px)',
        },
      }}
    >
      <CardContent sx={{ p: 3, '&:last-child': { pb: 3 } }}>
        {/* Top row: icon + trend */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            mb: 2.5,
          }}
        >
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: '12px',
              bgcolor: iconBg,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
            aria-hidden="true"
          >
            <Icon sx={{ color: iconColor, fontSize: 22 }} />
          </Box>

          {trend != null && (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 0.3,
                color: trendClr,
                bgcolor: `${trendClr}18`,
                borderRadius: '6px',
                px: 0.75,
                py: 0.35,
              }}
              aria-label={`Trend: ${formatTrend(trend)} ${trendLabel}`}
            >
              <TrendIcon change={trend} />
              <Typography variant="caption" fontWeight={700} sx={{ fontSize: '0.7rem' }}>
                {formatTrend(trend)}
              </Typography>
            </Box>
          )}
        </Box>

        {/* Metric */}
        <Typography
          variant="h3"
          fontWeight={800}
          sx={{ lineHeight: 1.1, mb: 0.5, letterSpacing: '-0.02em' }}
          aria-label={`${label}: ${value}`}
        >
          {formatCompactNumber(value)}
        </Typography>

        <Typography variant="body2" color="text.secondary" fontWeight={500}>
          {label}
        </Typography>

        {trend != null && (
          <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 0.5 }}>
            {trend >= 0 ? '+' : ''}
            {trend} {trendLabel}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}
