/**
 * @fileoverview AnalyticsKpiCard — a single KPI summary tile for the analytics
 * dashboard. Matches the PeopleCore HR design language.
 */

import React from 'react';
import { Box, Skeleton, Typography, useTheme } from '@mui/material';
import TrendingUpRoundedIcon from '@mui/icons-material/TrendingUpRounded';
import TrendingDownRoundedIcon from '@mui/icons-material/TrendingDownRounded';
import TrendingFlatRoundedIcon from '@mui/icons-material/TrendingFlatRounded';

/**
 * @typedef {Object} AnalyticsKpiCardProps
 * @property {React.ReactNode} icon          - MUI icon element.
 * @property {string}          label         - Card title.
 * @property {string|number}   value         - Primary metric value.
 * @property {string}          [subValue]    - Secondary line of detail.
 * @property {number}          [trend]       - Positive = up, negative = down, 0/null = flat.
 * @property {string}          [trendLabel]  - Human-readable trend label.
 * @property {boolean}         [loading]     - Show skeleton while loading.
 * @property {string}          [color]       - Accent color hex (optional).
 */

/**
 * KPI card tile for the analytics dashboard.
 *
 * @param {AnalyticsKpiCardProps} props
 * @returns {JSX.Element}
 */
export default function AnalyticsKpiCard({
  icon,
  label,
  value,
  subValue,
  trend,
  trendLabel,
  loading = false,
  color,
}) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const accent = color || theme.palette.primary.main;

  const TrendIcon =
    trend > 0
      ? TrendingUpRoundedIcon
      : trend < 0
        ? TrendingDownRoundedIcon
        : TrendingFlatRoundedIcon;

  const trendColor =
    trend > 0
      ? theme.palette.success.main
      : trend < 0
        ? theme.palette.error.main
        : theme.palette.text.secondary;

  return (
    <Box
      sx={{
        p: { xs: 2, sm: 2.5 },
        borderRadius: 3,
        bgcolor: isDark ? 'rgba(19,28,46,0.7)' : 'rgba(255,255,255,0.85)',
        border: `1px solid ${isDark ? 'rgba(240,237,230,0.07)' : 'rgba(235,230,218,0.6)'}`,
        backdropFilter: 'blur(8px)',
        boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 12px rgba(26,35,66,0.05)',
        transition: 'transform 0.18s ease, box-shadow 0.18s ease',
        '&:hover': {
          transform: 'translateY(-2px)',
          boxShadow: isDark ? '0 6px 20px rgba(0,0,0,0.3)' : '0 6px 20px rgba(26,35,66,0.1)',
        },
        height: '100%',
      }}
      role="region"
      aria-label={`${label} KPI card`}
    >
      {loading ? (
        <>
          <Skeleton variant="circular" width={40} height={40} sx={{ mb: 1 }} />
          <Skeleton width="60%" height={20} />
          <Skeleton width="40%" height={36} sx={{ my: 0.5 }} />
          <Skeleton width="50%" height={16} />
        </>
      ) : (
        <>
          {/* Icon */}
          <Box
            sx={{
              display: 'inline-flex',
              p: 1.2,
              borderRadius: 2,
              bgcolor: `${accent}18`,
              mb: 1.5,
            }}
          >
            <Box sx={{ color: accent, display: 'flex', fontSize: 22 }}>{icon}</Box>
          </Box>

          {/* Label */}
          <Typography
            variant="caption"
            sx={{ color: 'text.secondary', fontWeight: 500, letterSpacing: 0.3 }}
            display="block"
          >
            {label}
          </Typography>

          {/* Value */}
          <Typography
            variant="h4"
            sx={{ fontWeight: 700, my: 0.5, lineHeight: 1.2 }}
            aria-label={`${label}: ${value}`}
          >
            {value}
          </Typography>

          {/* Sub-value */}
          {subValue && (
            <Typography variant="caption" sx={{ color: 'text.secondary' }} display="block">
              {subValue}
            </Typography>
          )}

          {/* Trend */}
          {trend !== undefined && trend !== null && (
            <Box
              sx={{
                mt: 1,
                display: 'flex',
                alignItems: 'center',
                gap: 0.5,
              }}
            >
              <TrendIcon sx={{ fontSize: 16, color: trendColor }} />
              {trendLabel && (
                <Typography variant="caption" sx={{ color: trendColor, fontWeight: 500 }}>
                  {trendLabel}
                </Typography>
              )}
            </Box>
          )}
        </>
      )}
    </Box>
  );
}
