/**
 * @fileoverview AiDashboardSummaryWidget — compact AI summary for the manager dashboard.
 *
 * Phase 7D: Shows a compact AI summary card on the manager dashboard.
 * All numbers come from the backend API. No calculations in the frontend.
 *
 * IMPORTANT: AI evaluation is advisory. This is a summary overview, not the
 * primary dashboard. Sensitive business metrics are computed on the backend only.
 */

import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import TrendingUpRoundedIcon from '@mui/icons-material/TrendingUpRounded';
import HourglassEmptyRoundedIcon from '@mui/icons-material/HourglassEmptyRounded';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import { useAiDashboardSummary } from '@/hooks/useTaskAiReviewHooks';

function StatItem({ icon, label, value, color = 'text.primary', testId }) {
  return (
    <Stack spacing={0.25} alignItems="flex-start" data-testid={testId}>
      <Stack direction="row" alignItems="center" spacing={0.5}>
        <Box sx={{ color }}>{icon}</Box>
        <Typography variant="caption" color="text.secondary">
          {label}
        </Typography>
      </Stack>
      <Typography variant="h6" fontWeight={700} color={color}>
        {value != null ? value : '—'}
      </Typography>
    </Stack>
  );
}

/**
 * Compact AI summary widget for the manager dashboard.
 *
 * @param {{ enabled?: boolean }} props
 */
export default function AiDashboardSummaryWidget({ enabled = true }) {
  const { data: summary, isLoading, isError } = useAiDashboardSummary({ enabled });

  if (!enabled) return null;

  return (
    <Card variant="outlined" sx={{ height: '100%' }} data-testid="ai-dashboard-summary-widget">
      <CardContent>
        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
          <AutoAwesomeRoundedIcon color="primary" fontSize="small" />
          <Typography variant="subtitle1" fontWeight={700}>
            AI Task Insights
          </Typography>
          <Chip label="Advisory" size="small" color="default" variant="outlined" sx={{ ml: 'auto', fontSize: '0.65rem' }} />
        </Stack>

        <Divider sx={{ mb: 2 }} />

        {isLoading && (
          <Stack spacing={1}>
            <Skeleton height={40} />
            <Skeleton height={40} />
            <Skeleton height={40} />
          </Stack>
        )}

        {isError && (
          <Typography variant="body2" color="text.secondary">
            AI summary unavailable.
          </Typography>
        )}

        {summary && (
          <Grid container spacing={2}>
            <Grid size={{ xs: 6 }}>
              <StatItem
                icon={<CheckCircleRoundedIcon fontSize="small" />}
                label="Evaluated"
                value={summary.totalEvaluated}
                color="success.main"
                testId="ai-stat-evaluated"
              />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <StatItem
                icon={<AutoAwesomeRoundedIcon fontSize="small" />}
                label="Avg Score"
                value={summary.averageScore != null ? `${summary.averageScore}%` : '—'}
                color="primary.main"
                testId="ai-stat-avg-score"
              />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <StatItem
                icon={<TrendingUpRoundedIcon fontSize="small" />}
                label="Improving"
                value={summary.employeesImproving}
                color="success.main"
                testId="ai-stat-improving"
              />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <StatItem
                icon={<WarningAmberRoundedIcon fontSize="small" />}
                label="Need Attention"
                value={summary.employeesNeedingAttention}
                color={summary.employeesNeedingAttention > 0 ? 'warning.main' : 'text.secondary'}
                testId="ai-stat-attention"
              />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <StatItem
                icon={<HourglassEmptyRoundedIcon fontSize="small" />}
                label="Awaiting"
                value={summary.submissionsAwaitingEvaluation}
                color="info.main"
                testId="ai-stat-awaiting"
              />
            </Grid>
            <Grid size={{ xs: 6 }}>
              <StatItem
                icon={<ErrorOutlineRoundedIcon fontSize="small" />}
                label="Failed"
                value={summary.failedEvaluations}
                color={summary.failedEvaluations > 0 ? 'error.main' : 'text.secondary'}
                testId="ai-stat-failed"
              />
            </Grid>
          </Grid>
        )}
      </CardContent>
    </Card>
  );
}
