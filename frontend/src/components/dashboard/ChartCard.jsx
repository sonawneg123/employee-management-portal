/**
 * @fileoverview ChartCard — section card wrapper optimised for Recharts.
 *
 * Extends {@link SectionCard} with a fixed-height responsive container
 * that gives Recharts a reliable bounding box to render within.
 * Also handles the loading skeleton and empty state for chart sections.
 */

import React from 'react';
import { Box, Skeleton, Typography } from '@mui/material';
import BarChartIcon from '@mui/icons-material/BarChart';
import SectionCard from './SectionCard';

/**
 * @typedef {Object} ChartCardProps
 * @property {string}          title        - Card section title.
 * @property {string}          [subtitle]
 * @property {boolean}         [showRefresh]
 * @property {() => void}      [onRefresh]
 * @property {boolean}         [isFetching]
 * @property {boolean}         [loading]    - Shows skeleton placeholder when true.
 * @property {boolean}         [isEmpty]    - Shows empty state when true (and not loading).
 * @property {string}          [emptyText]  - Message shown in the empty state.
 * @property {number}          [height=300] - Fixed chart container height in pixels.
 * @property {React.ReactNode} children     - The Recharts chart element.
 * @property {React.ReactNode} [action]
 */

/**
 * Responsive card wrapper for Recharts chart components.
 *
 * @param {ChartCardProps} props
 * @returns {JSX.Element}
 */
export default function ChartCard({
  title,
  subtitle,
  showRefresh = true,
  onRefresh,
  isFetching = false,
  loading = false,
  isEmpty = false,
  emptyText = 'No data available',
  height = 300,
  children,
  action,
}) {
  const renderBody = () => {
    if (loading) {
      return (
        <Skeleton
          variant="rectangular"
          width="100%"
          height={height}
          sx={{ borderRadius: 2 }}
          aria-label="Loading chart"
        />
      );
    }

    if (isEmpty) {
      return (
        <Box
          sx={{
            height,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 1,
            color: 'text.disabled',
          }}
          role="status"
          aria-label={emptyText}
        >
          <BarChartIcon sx={{ fontSize: 48, opacity: 0.3 }} />
          <Typography variant="body2">{emptyText}</Typography>
        </Box>
      );
    }

    return (
      <Box sx={{ width: '100%', height }} aria-label={`${title} chart`}>
        {children}
      </Box>
    );
  };

  return (
    <SectionCard
      title={title}
      subtitle={subtitle}
      showRefresh={showRefresh}
      onRefresh={onRefresh}
      isFetching={isFetching}
      action={action}
      contentSx={{ p: 2 }}
    >
      {renderBody()}
    </SectionCard>
  );
}
