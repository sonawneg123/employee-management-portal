/**
 * @fileoverview ManagerAiTrendSection — AI score trend and insights for managers.
 *
 * Phase 7D: Shows managers:
 *   - Score trend history (Previous → Current, with change)
 *   - Improving / Stable / Declining indicator
 *   - Simple inline score visualization
 *   - AI task insights (common issues, strengths, suggestions)
 *
 * IMPORTANT:
 *   - Manager-only — employees never see this section
 *   - AI evaluation is advisory; manager always decides
 *   - No new AI API calls — reads stored evaluation data
 *   - Failed evaluations are excluded from trend classification
 */

import React, { useState } from 'react';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  Collapse,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
  Skeleton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import TrendingUpRoundedIcon from '@mui/icons-material/TrendingUpRounded';
import TrendingFlatRoundedIcon from '@mui/icons-material/TrendingFlatRounded';
import TrendingDownRoundedIcon from '@mui/icons-material/TrendingDownRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import LightbulbOutlinedIcon from '@mui/icons-material/LightbulbOutlined';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded';
import ExpandLessRoundedIcon from '@mui/icons-material/ExpandLessRounded';
import { useAiScoreTrend, useAiTaskInsights } from '@/hooks/useTaskAiReviewHooks';

// ── Helpers ────────────────────────────────────────────────────────────────────

function formatDate(isoString) {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function TrendChip({ direction }) {
  const configs = {
    IMPROVING: {
      label: 'Improving',
      color: 'success',
      icon: <TrendingUpRoundedIcon fontSize="small" />,
    },
    STABLE: {
      label: 'Stable',
      color: 'default',
      icon: <TrendingFlatRoundedIcon fontSize="small" />,
    },
    DECLINING: {
      label: 'Needs Attention',
      color: 'warning',
      icon: <TrendingDownRoundedIcon fontSize="small" />,
    },
    INSUFFICIENT_DATA: {
      label: 'Not Enough Data',
      color: 'default',
      icon: <InfoOutlinedIcon fontSize="small" />,
    },
  };
  const cfg = configs[direction] ?? configs.INSUFFICIENT_DATA;
  return <Chip icon={cfg.icon} label={cfg.label} color={cfg.color} size="small" />;
}

// ── Inline score trend visualization ─────────────────────────────────────────

function ScoreTrendViz({ scoreHistory, hasTrendData }) {
  if (!hasTrendData || !scoreHistory?.length) {
    return (
      <Typography variant="caption" color="text.secondary" data-testid="no-trend-message">
        Not enough evaluation history for a trend.
      </Typography>
    );
  }

  return (
    <Box data-testid="score-trend-viz">
      <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
        Score progression
      </Typography>
      <Stack direction="row" alignItems="center" flexWrap="wrap" gap={0.5}>
        {scoreHistory.map((point, idx) => (
          <React.Fragment key={point.reviewId}>
            <Tooltip
              title={`Submission #${point.submissionNumber} — ${formatDate(point.evaluatedAt)}`}
              placement="top"
            >
              <Paper
                variant="outlined"
                sx={{
                  px: 1,
                  py: 0.5,
                  minWidth: 40,
                  textAlign: 'center',
                  bgcolor:
                    point.overallScore >= 75
                      ? 'success.50'
                      : point.overallScore >= 50
                        ? 'warning.50'
                        : 'error.50',
                  borderColor:
                    point.overallScore >= 75
                      ? 'success.light'
                      : point.overallScore >= 50
                        ? 'warning.light'
                        : 'error.light',
                  cursor: 'default',
                }}
              >
                <Typography variant="caption" fontWeight={700}>
                  {point.overallScore}
                </Typography>
              </Paper>
            </Tooltip>
            {idx < scoreHistory.length - 1 && (
              <Typography variant="caption" color="text.secondary">
                →
              </Typography>
            )}
          </React.Fragment>
        ))}
      </Stack>
    </Box>
  );
}

// ── Score summary card ────────────────────────────────────────────────────────

function ScoreSummaryCard({ trend }) {
  const hasData = trend.hasTrendData;
  const showChange = hasData && trend.latestScoreChange != null;
  const changeColor = !showChange
    ? 'text.secondary'
    : trend.latestScoreChange > 5
      ? 'success.main'
      : trend.latestScoreChange < -5
        ? 'error.main'
        : 'text.secondary';

  return (
    <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center', mb: 2 }}>
      {trend.latestScore != null && (
        <Box data-testid="latest-score">
          <Typography variant="caption" color="text.secondary" display="block">
            Current Score
          </Typography>
          <Typography variant="h4" fontWeight={700} color="primary">
            {trend.latestScore}
          </Typography>
        </Box>
      )}

      {trend.previousScore != null && (
        <>
          <Typography variant="h5" color="text.disabled" sx={{ alignSelf: 'center' }}>
            →
          </Typography>
          <Box data-testid="previous-score">
            <Typography variant="caption" color="text.secondary" display="block">
              Previous
            </Typography>
            <Typography variant="h4" fontWeight={400} color="text.secondary">
              {trend.previousScore}
            </Typography>
          </Box>
        </>
      )}

      {showChange && (
        <Box data-testid="score-change">
          <Typography variant="caption" color="text.secondary" display="block">
            Change
          </Typography>
          <Typography variant="h5" fontWeight={700} color={changeColor}>
            {trend.latestScoreChange > 0 ? '+' : ''}
            {trend.latestScoreChange}
          </Typography>
        </Box>
      )}

      {hasData && <TrendChip direction={trend.trendDirection} />}
    </Box>
  );
}

// ── Main component ─────────────────────────────────────────────────────────────

/**
 * Manager AI trend section — shows score trends and insights for a task.
 *
 * @param {{ taskId: string | null | undefined }} props
 */
export default function ManagerAiTrendSection({ taskId }) {
  const [showInsights, setShowInsights] = useState(false);

  const { data: trend, isLoading: isTrendLoading } = useAiScoreTrend(taskId);

  const { data: insights, isLoading: isInsightsLoading } = useAiTaskInsights(taskId, {
    enabled: Boolean(taskId) && showInsights,
  });

  if (!taskId) return null;

  if (isTrendLoading) {
    return (
      <Card variant="outlined" sx={{ mt: 2 }}>
        <CardContent>
          <Skeleton width="40%" height={24} />
          <Skeleton width="80%" height={40} sx={{ mt: 1 }} />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card variant="outlined" sx={{ mt: 2 }} data-testid="manager-ai-trend-section">
      <CardContent>
        {/* Header */}
        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
          <AutoAwesomeRoundedIcon color="primary" fontSize="small" />
          <Typography variant="subtitle1" fontWeight={700}>
            AI Score Trend
          </Typography>
        </Stack>

        <Divider sx={{ mb: 2 }} />

        {/* No data */}
        {!trend && (
          <Typography variant="body2" color="text.secondary">
            No AI evaluation data available for this task.
          </Typography>
        )}

        {trend && (
          <>
            {/* Score summary */}
            <ScoreSummaryCard trend={trend} />

            {/* Inline visualization */}
            <ScoreTrendViz scoreHistory={trend.scoreHistory} hasTrendData={trend.hasTrendData} />

            {/* Advisory notice */}
            <Alert
              severity="info"
              icon={<InfoOutlinedIcon fontSize="small" />}
              variant="outlined"
              sx={{ mt: 2, py: 0.5 }}
            >
              <Typography variant="caption">
                AI evaluation is advisory. Manager decisions always take precedence. Failed
                evaluations are excluded from trend calculations.
              </Typography>
            </Alert>

            {/* AI Insights toggle */}
            {trend.scoreHistory?.length > 0 && (
              <Box sx={{ mt: 2 }}>
                <Box
                  component="button"
                  onClick={() => setShowInsights((v) => !v)}
                  data-testid="toggle-insights-btn"
                  sx={{
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                    color: 'primary.main',
                    p: 0,
                    '&:hover': { textDecoration: 'underline' },
                  }}
                >
                  {showInsights ? (
                    <ExpandLessRoundedIcon fontSize="small" />
                  ) : (
                    <ExpandMoreRoundedIcon fontSize="small" />
                  )}
                  <Typography variant="body2" fontWeight={600}>
                    {showInsights ? 'Hide AI Insights' : 'Show AI Insights'}
                  </Typography>
                </Box>

                <Collapse in={showInsights}>
                  <Box sx={{ mt: 2 }} data-testid="ai-insights-section">
                    {isInsightsLoading && <Skeleton height={80} />}

                    {insights && (
                      <Stack spacing={2}>
                        {/* Common issues */}
                        {insights.commonIssues?.length > 0 && (
                          <Box>
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              fontWeight={600}
                              gutterBottom
                              display="block"
                            >
                              Common Issues
                            </Typography>
                            <List dense disablePadding>
                              {insights.commonIssues.map((issue, i) => (
                                <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                                  <ListItemIcon sx={{ minWidth: 20 }}>
                                    <WarningAmberRoundedIcon fontSize="small" color="warning" />
                                  </ListItemIcon>
                                  <ListItemText
                                    primary={issue}
                                    primaryTypographyProps={{ variant: 'body2' }}
                                  />
                                </ListItem>
                              ))}
                            </List>
                          </Box>
                        )}

                        {/* Most recent strengths */}
                        {insights.mostRecentStrengths?.length > 0 && (
                          <Box>
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              fontWeight={600}
                              gutterBottom
                              display="block"
                            >
                              Most Recent Strengths
                            </Typography>
                            <List dense disablePadding>
                              {insights.mostRecentStrengths.map((s, i) => (
                                <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                                  <ListItemIcon sx={{ minWidth: 20 }}>
                                    <CheckCircleOutlineRoundedIcon
                                      fontSize="small"
                                      color="success"
                                    />
                                  </ListItemIcon>
                                  <ListItemText
                                    primary={s}
                                    primaryTypographyProps={{ variant: 'body2' }}
                                  />
                                </ListItem>
                              ))}
                            </List>
                          </Box>
                        )}

                        {/* Most recent suggestions */}
                        {insights.mostRecentSuggestions?.length > 0 && (
                          <Box>
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              fontWeight={600}
                              gutterBottom
                              display="block"
                            >
                              AI Improvement Suggestions
                              <Typography
                                component="span"
                                variant="caption"
                                color="text.disabled"
                                sx={{ ml: 1 }}
                              >
                                (advisory — not mandatory requirements)
                              </Typography>
                            </Typography>
                            <List dense disablePadding>
                              {insights.mostRecentSuggestions.map((s, i) => (
                                <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                                  <ListItemIcon sx={{ minWidth: 20 }}>
                                    <LightbulbOutlinedIcon fontSize="small" color="info" />
                                  </ListItemIcon>
                                  <ListItemText
                                    primary={s}
                                    primaryTypographyProps={{ variant: 'body2' }}
                                  />
                                </ListItem>
                              ))}
                            </List>
                          </Box>
                        )}

                        <Typography variant="caption" color="text.secondary">
                          Based on {insights.completedEvaluations} completed AI evaluation
                          {insights.completedEvaluations !== 1 ? 's' : ''}. Average score:{' '}
                          {insights.averageScore != null ? `${insights.averageScore}%` : '—'}
                        </Typography>
                      </Stack>
                    )}
                  </Box>
                </Collapse>
              </Box>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
