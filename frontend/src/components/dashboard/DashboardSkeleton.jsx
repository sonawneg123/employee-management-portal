/**
 * @fileoverview DashboardSkeleton — full-page loading placeholder.
 *
 * Rendered while all three dashboard queries are in their initial loading
 * state. Mimics the layout of the real dashboard so that there is no
 * layout shift when data arrives.
 */

import React from 'react';
import { Box, Grid, Skeleton, Card, CardContent } from '@mui/material';

/**
 * Skeleton placeholder for a summary stat card.
 *
 * @returns {JSX.Element}
 */
function StatSkeleton() {
  return (
    <Card>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Skeleton variant="rectangular" width={48} height={48} sx={{ borderRadius: 2 }} />
          <Skeleton variant="text" width={50} />
        </Box>
        <Skeleton variant="text" width="45%" height={36} />
        <Skeleton variant="text" width="65%" />
      </CardContent>
    </Card>
  );
}

/**
 * Skeleton placeholder for a section card.
 *
 * @param {{ height?: number }} props
 * @returns {JSX.Element}
 */
function SectionSkeleton({ height = 240 }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        <Skeleton variant="text" width="40%" height={28} sx={{ mb: 1 }} />
        <Skeleton variant="rectangular" width="100%" height={height} sx={{ borderRadius: 2 }} />
      </CardContent>
    </Card>
  );
}

/**
 * Full-page dashboard loading skeleton.
 *
 * @returns {JSX.Element}
 */
export default function DashboardSkeleton() {
  return (
    <Box aria-busy="true" aria-label="Loading dashboard">
      {/* Welcome card skeleton */}
      <Skeleton variant="rectangular" width="100%" height={100} sx={{ borderRadius: 3, mb: 3 }} />

      {/* Stat cards row */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {[0, 1, 2, 3].map((i) => (
          <Grid key={i} size={{ xs: 12, sm: 6, lg: 3 }}>
            <StatSkeleton />
          </Grid>
        ))}
      </Grid>

      {/* Charts row */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 8 }}>
          <SectionSkeleton height={300} />
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <SectionSkeleton height={300} />
        </Grid>
      </Grid>

      {/* Widgets row */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <SectionSkeleton height={240} />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <SectionSkeleton height={240} />
        </Grid>
      </Grid>
    </Box>
  );
}
