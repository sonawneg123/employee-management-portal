/**
 * @fileoverview SummaryCard — premium KPI metric card with trend indicator.
 *
 * Displays a single numeric metric with icon, trend delta, and subtle hover animation.
 * Uses a colored icon container and clean typography hierarchy.
 * Premium SaaS design — navy, cream, gold accent.
 */

import React from 'react';
import { Box, Card, CardContent, Skeleton, Typography, useTheme } from '@mui/material';
import TrendingUpRoundedIcon from '@mui/icons-material/TrendingUpRounded';
import TrendingDownRoundedIcon from '@mui/icons-material/TrendingDownRounded';
import TrendingFlatRoundedIcon from '@mui/icons-material/TrendingFlatRounded';
import { formatCompactNumber, formatTrend, trendColor } from '@/utils/dashboardFormatters';

/** @param {{ change: number }} props */
function TrendIcon({ change }) {
  if (change > 0) return <TrendingUpRoundedIcon sx={{ fontSize: 11 }} />;
  if (change < 0) return <TrendingDownRoundedIcon sx={{ fontSize: 11 }} />;
  return <TrendingFlatRoundedIcon sx={{ fontSize: 11 }} />;
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
 * Single KPI stat card — premium SaaS style.
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
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  if (loading) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent sx={{ p: 3 }}>
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              mb: 2.5,
            }}
          >
            <Skeleton variant="rounded" width={52} height={52} sx={{ borderRadius: '16px' }} />
            <Skeleton variant="rounded" width={54} height={22} sx={{ borderRadius: '8px' }} />
          </Box>
          <Skeleton variant="text" width="45%" height={48} sx={{ mb: 0.5 }} />
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
          boxShadow: isDark ? '0 16px 48px rgba(0,0,0,0.55)' : '0 12px 44px rgba(26,35,66,0.12)',
          transform: 'translateY(-3px)',
        },
        '@media (prefers-reduced-motion: reduce)': {
          transition: 'none',
          '&:hover': { transform: 'none' },
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
          {/* Icon container — rounded square */}
          <Box
            sx={{
              width: 52,
              height: 52,
              borderRadius: '16px',
              bgcolor: iconBg,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
              boxShadow: `0 4px 12px ${iconBg}`,
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
                gap: 0.4,
                color: trendClr,
                bgcolor: `${trendClr}14`,
                borderRadius: '8px',
                px: 0.85,
                py: 0.4,
                border: `1px solid ${trendClr}22`,
              }}
              aria-label={`Trend: ${formatTrend(trend)} ${trendLabel}`}
            >
              <TrendIcon change={trend} />
              <Typography variant="caption" fontWeight={700} sx={{ fontSize: '0.68rem' }}>
                {formatTrend(trend)}
              </Typography>
            </Box>
          )}
        </Box>

        {/* Metric value */}
        <Typography
          variant="h3"
          fontWeight={800}
          sx={{
            lineHeight: 1.1,
            mb: 0.5,
            letterSpacing: '-0.03em',
            fontSize: { xs: '1.75rem', sm: '2rem' },
            color: isDark ? '#F0EDE6' : '#1A2342',
          }}
          aria-label={`${label}: ${value}`}
        >
          {formatCompactNumber(value)}
        </Typography>

        <Typography
          variant="body2"
          fontWeight={500}
          sx={{ color: isDark ? 'rgba(240,237,230,0.55)' : '#7A7468' }}
        >
          {label}
        </Typography>

        {trend != null && (
          <Typography
            variant="caption"
            sx={{
              display: 'block',
              mt: 0.75,
              color: isDark ? 'rgba(240,237,230,0.35)' : '#9CA3AF',
            }}
          >
            {trend >= 0 ? '+' : ''}
            {trend} {trendLabel}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}
