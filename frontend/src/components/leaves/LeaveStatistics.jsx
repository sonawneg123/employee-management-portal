/**
 * @fileoverview LeaveStatistics — summary counts by type and status.
 *
 * Rendered as a row of Chip-like stat tiles giving HR/Admin a quick
 * breakdown of the current list dataset.
 */

import React from 'react';
import { Box, Card, CardContent, Chip, Grid, Skeleton, Typography } from '@mui/material';
import { aggregateByType, aggregateByStatus } from '@/utils/leaveCalculations';
import { formatLeaveType, formatLeaveStatus } from '@/utils/leaveFormatters';
import { LEAVE_STATUS_MAP, LEAVE_TYPE_MAP } from '@/constants/leaveConstants';

/**
 * @typedef {Object} LeaveStatisticsProps
 * @property {import('@/services/leaveApi').LeaveRequestResponse[]} leaves
 * @property {boolean} [isLoading]
 */

/**
 * Summary statistics for the currently loaded leave dataset.
 *
 * @param {LeaveStatisticsProps} props
 * @returns {JSX.Element}
 */
export default function LeaveStatistics({ leaves = [], isLoading = false }) {
  const byStatus = aggregateByStatus(leaves);
  const byType = aggregateByType(leaves);

  if (isLoading) {
    return (
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {[0, 1, 2, 3].map((i) => (
              <Skeleton
                key={i}
                variant="rectangular"
                width={90}
                height={32}
                sx={{ borderRadius: 4 }}
              />
            ))}
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (!leaves.length) return null;

  return (
    <Card sx={{ mb: 2 }}>
      <CardContent sx={{ pb: '12px !important' }}>
        <Grid container spacing={2}>
          {/* By status */}
          <Grid size={{ xs: 12, md: 6 }}>
            <Typography
              variant="caption"
              color="text.secondary"
              fontWeight={600}
              sx={{ mb: 1, display: 'block' }}
            >
              BY STATUS
            </Typography>
            <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
              {byStatus.map(({ status, count }) => {
                const meta = LEAVE_STATUS_MAP[status];
                return (
                  <Chip
                    key={status}
                    label={`${formatLeaveStatus(status)}: ${count}`}
                    color={meta?.color ?? 'default'}
                    size="small"
                    sx={{ fontWeight: 600 }}
                    aria-label={`${status} count: ${count}`}
                  />
                );
              })}
            </Box>
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <Typography
              variant="caption"
              color="text.secondary"
              fontWeight={600}
              sx={{ mb: 1, display: 'block' }}
            >
              BY TYPE
            </Typography>
            <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
              {byType.map(({ type, count }) => {
                const meta = LEAVE_TYPE_MAP[type];
                return (
                  <Chip
                    key={type}
                    label={`${meta?.icon ?? ''} ${formatLeaveType(type)}: ${count}`}
                    variant="outlined"
                    color={meta?.color ?? 'default'}
                    size="small"
                    sx={{ fontWeight: 600 }}
                    aria-label={`${type} count: ${count}`}
                  />
                );
              })}
            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}
