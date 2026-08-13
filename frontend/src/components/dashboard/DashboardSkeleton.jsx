/**
 * @fileoverview DashboardSkeleton — full-page loading placeholder.
 *
 * Mirrors the real dashboard layout to prevent layout shift when data arrives.
 */

import React from 'react';
import { Box, Grid, Skeleton, Card, CardContent } from '@mui/material';

function StatSkeleton() {
  return (
    <Card>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2.5 }}>
          <Skeleton variant="rounded" width={48} height={48} sx={{ borderRadius: '12px' }} />
          <Skeleton variant="rounded" width={56} height={24} sx={{ borderRadius: '6px' }} />
        </Box>
        <Skeleton variant="text" width="45%" height={40} />
        <Skeleton variant="text" width="65%" height={18} />
      </CardContent>
    </Card>
  );
}

function SectionSkeleton({ height = 240 }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        <Skeleton variant="text" width="45%" height={26} sx={{ mb: 0.5 }} />
        <Skeleton variant="text" width="65%" height={18} sx={{ mb: 2 }} />
        <Skeleton variant="rounded" width="100%" height={height} sx={{ borderRadius: '12px' }} />
      </CardContent>
    </Card>
  );
}

/**
 * Full-page dashboard skeleton.
 *
 * @returns {JSX.Element}
 */
export default function DashboardSkeleton() {
  return (
    <Box aria-busy="true" aria-label="Loading dashboard">
      {/* Header skeleton */}
      <Box
        sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}
      >
        <Box>
          <Skeleton variant="text" width={200} height={40} />
          <Skeleton variant="text" width={160} height={18} />
        </Box>
        <Skeleton variant="rounded" width={120} height={34} sx={{ borderRadius: '8px' }} />
      </Box>

      {/* Welcome card skeleton */}
      <Skeleton variant="rounded" width="100%" height={86} sx={{ borderRadius: '16px', mb: 3 }} />

      {/* Stat cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {[0, 1, 2, 3].map((i) => (
          <Grid key={i} size={{ xs: 12, sm: 6, lg: 3 }}>
            <StatSkeleton />
          </Grid>
        ))}
      </Grid>

      {/* Charts */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <SectionSkeleton height={300} />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <SectionSkeleton height={300} />
        </Grid>
      </Grid>

      {/* Widgets */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <SectionSkeleton height={280} />
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Grid container spacing={3} direction="column">
            <Grid size={12}>
              <SectionSkeleton height={120} />
            </Grid>
            <Grid size={12}>
              <SectionSkeleton height={120} />
            </Grid>
          </Grid>
        </Grid>
      </Grid>
    </Box>
  );
}
