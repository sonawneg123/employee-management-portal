/**
 * @fileoverview TaskAiEvaluationSection — Phase 7B AI Evaluation panel.
 *
 * Shown on the manager task detail page below the submission review section.
 *
 * Features:
 *  - Requires an existing submission (AI evaluation is submission-dependent)
 *  - Shows status chip for PENDING / PROCESSING / COMPLETED / FAILED
 *  - "Run AI Evaluation" and "Re-run AI Evaluation" buttons
 *  - Loading skeleton while evaluating
 *  - Full evaluation result card:
 *      · Overall score (completion + quality)
 *      · Requirement breakdown
 *      · AI Recommendation (advisory only)
 *      · Manager Summary
 *      · Strengths / Issues / Suggested Changes
 *  - Evaluation history table
 *  - No raw JSON exposed — all AI content rendered as plain text (XSS safe)
 *
 * Security: ADMIN, HR, MANAGER only (enforced by backend).
 * AI content is always rendered as plain React text, never as raw HTML.
 */

import React, { useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Collapse,
  Divider,
  LinearProgress,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import PlayArrowRoundedIcon from '@mui/icons-material/PlayArrowRounded';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import ErrorRoundedIcon from '@mui/icons-material/ErrorRounded';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import HourglassTopRoundedIcon from '@mui/icons-material/HourglassTopRounded';
import CheckRoundedIcon from '@mui/icons-material/CheckRounded';
import PriorityHighRoundedIcon from '@mui/icons-material/PriorityHighRounded';
import LightbulbRoundedIcon from '@mui/icons-material/LightbulbRounded';
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded';
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded';
import ExpandLessRoundedIcon from '@mui/icons-material/ExpandLessRounded';

import { useLatestAiReview, useAllAiReviews, useRunAiReview } from '@/hooks/useTaskAiReviewHooks';

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Safely parse the structuredAnalysisJson field from a review response.
 *
 * @param {string|null|undefined} json
 * @returns {import('../../services/taskAiReviewApi').TaskAiAnalysis|null}
 */
function parseAnalysis(json) {
  if (!json) return null;
  try {
    return JSON.parse(json);
  } catch {
    return null;
  }
}

/**
 * Format a date string into a human-readable short form.
 *
 * @param {string|null|undefined} isoString
 * @returns {string}
 */
function formatDate(isoString) {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// ── Status chip ───────────────────────────────────────────────────────────────

function AiStatusChip({ status }) {
  if (status === 'COMPLETED') {
    return (
      <Chip
        icon={<CheckCircleRoundedIcon />}
        label="Evaluation Complete"
        color="success"
        size="small"
        variant="outlined"
      />
    );
  }
  if (status === 'FAILED') {
    return (
      <Chip
        icon={<ErrorRoundedIcon />}
        label="Evaluation Failed"
        color="error"
        size="small"
        variant="outlined"
      />
    );
  }
  if (status === 'PENDING' || status === 'PROCESSING') {
    return (
      <Chip
        icon={<HourglassTopRoundedIcon />}
        label={status === 'PROCESSING' ? 'Evaluating…' : 'Queued'}
        color="info"
        size="small"
        variant="outlined"
      />
    );
  }
  return null;
}

// ── Score bar ─────────────────────────────────────────────────────────────────

function ScoreBar({ label, value }) {
  const pct = Math.max(0, Math.min(100, value ?? 0));
  const color = pct >= 80 ? 'success' : pct >= 60 ? 'warning' : 'error';
  return (
    <Box sx={{ mb: 1 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 0.25 }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="caption" fontWeight={700}>{pct}</Typography>
      </Stack>
      <LinearProgress
        variant="determinate"
        value={pct}
        color={color}
        sx={{ height: 6, borderRadius: 3 }}
      />
    </Box>
  );
}

// ── Recommendation chip ───────────────────────────────────────────────────────

function RecommendationBadge({ action }) {
  if (action === 'APPROVE') {
    return (
      <Chip
        icon={<CheckCircleRoundedIcon />}
        label="Approve"
        color="success"
        size="small"
      />
    );
  }
  if (action === 'REQUEST_CHANGES') {
    return (
      <Chip
        icon={<EditIcon />}
        label="Request Changes"
        color="warning"
        size="small"
      />
    );
  }
  // MANUAL_REVIEW or unknown
  return (
    <Chip
      icon={<WarningAmberRoundedIcon />}
      label="Review Manually"
      color="default"
      size="small"
    />
  );
}

// We don't import EditRoundedIcon at the top to keep imports clean — use inline icon
function EditIcon() {
  return (
    <svg style={{ width: 16, height: 16, fill: 'currentColor' }} viewBox="0 0 24 24">
      <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm18-12.38a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
    </svg>
  );
}

// ── Full evaluation result ────────────────────────────────────────────────────

function EvaluationResult({ review }) {
  const analysis = useMemo(() => parseAnalysis(review.structuredAnalysisJson), [review.structuredAnalysisJson]);
  const [showRequirements, setShowRequirements] = useState(false);

  const completionScore = review.completionScore ?? null;
  const qualityScore = review.qualityScore ?? null;

  const strengths = analysis?.qualityAssessment?.strengths ?? [];
  const weaknesses = analysis?.qualityAssessment?.weaknesses ?? [];
  const issues = analysis?.issues ?? [];
  const suggestions = analysis?.modificationSuggestions ?? [];
  const requirements = analysis?.requirements ?? [];

  // Overall summary: prefer managerSummary field, fall back to analysis.managerSummary
  const summary = review.managerSummary || analysis?.managerSummary || analysis?.overallAssessment;

  return (
    <Box>
      {/* ── Scores ──────────────────────────────────────────────────── */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="subtitle2" fontWeight={700} gutterBottom>
          Overall Score
        </Typography>
        <Stack direction="row" spacing={3} alignItems="center" sx={{ mb: 1 }}>
          {completionScore !== null && (
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="h3" fontWeight={800} color="primary.main" lineHeight={1}>
                {completionScore}
              </Typography>
              <Typography variant="caption" color="text.secondary">/ 100</Typography>
              <Typography variant="caption" display="block" color="text.secondary">
                Completion
              </Typography>
            </Box>
          )}
          {qualityScore !== null && (
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="h3" fontWeight={800} color="secondary.main" lineHeight={1}>
                {qualityScore}
              </Typography>
              <Typography variant="caption" color="text.secondary">/ 100</Typography>
              <Typography variant="caption" display="block" color="text.secondary">
                Quality
              </Typography>
            </Box>
          )}
          {review.confidence !== null && review.confidence !== undefined && (
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="h5" fontWeight={600} color="text.secondary" lineHeight={1}>
                {review.confidence}%
              </Typography>
              <Typography variant="caption" display="block" color="text.secondary">
                AI Confidence
              </Typography>
            </Box>
          )}
        </Stack>

        {/* Score bars */}
        <Box sx={{ maxWidth: 360 }}>
          {completionScore !== null && <ScoreBar label="Task Completion" value={completionScore} />}
          {qualityScore !== null && <ScoreBar label="Quality" value={qualityScore} />}
          {analysis?.qualityAssessment?.score != null && completionScore !== analysis.qualityAssessment.score && (
            <ScoreBar label="Quality Assessment" value={analysis.qualityAssessment.score} />
          )}
        </Box>
      </Box>

      <Divider sx={{ mb: 2 }} />

      {/* ── AI Recommendation ────────────────────────────────────────── */}
      {review.recommendedAction && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" fontWeight={700} gutterBottom>
            AI Recommendation
          </Typography>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
            <RecommendationBadge action={review.recommendedAction} />
          </Stack>
          <Alert severity="info" icon={<SmartToyRoundedIcon />} sx={{ mt: 1 }}>
            <Typography variant="caption">
              <strong>Advisory only.</strong> This AI recommendation does not automatically
              approve or reject the submission. You make the final decision.
            </Typography>
          </Alert>
        </Box>
      )}

      {/* ── Summary ─────────────────────────────────────────────────── */}
      {summary && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" fontWeight={700} gutterBottom>
            AI Summary
          </Typography>
          <Box
            sx={{
              bgcolor: 'grey.50',
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 1,
              p: 2,
            }}
          >
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
              {/* Plain text rendering — never dangerouslySetInnerHTML */}
              {summary}
            </Typography>
          </Box>
        </Box>
      )}

      {/* ── Strengths ────────────────────────────────────────────────── */}
      {strengths.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" fontWeight={700} gutterBottom>
            Strengths
          </Typography>
          <List dense disablePadding>
            {strengths.map((item, i) => (
              <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                <ListItemIcon sx={{ minWidth: 28 }}>
                  <CheckRoundedIcon fontSize="small" color="success" />
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="body2">{item}</Typography>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Box>
      )}

      {/* ── Issues ──────────────────────────────────────────────────── */}
      {(issues.length > 0 || weaknesses.length > 0) && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" fontWeight={700} gutterBottom>
            Issues Found
          </Typography>
          <List dense disablePadding>
            {[...issues, ...weaknesses].map((item, i) => (
              <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                <ListItemIcon sx={{ minWidth: 28 }}>
                  <PriorityHighRoundedIcon fontSize="small" color="warning" />
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="body2">{item}</Typography>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Box>
      )}

      {/* ── Suggested Changes ────────────────────────────────────────── */}
      {suggestions.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" fontWeight={700} gutterBottom>
            Suggested Improvements
          </Typography>
          <List dense disablePadding>
            {suggestions.map((item, i) => (
              <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                <ListItemIcon sx={{ minWidth: 28 }}>
                  <LightbulbRoundedIcon fontSize="small" color="primary" />
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography variant="body2">
                      {i + 1}. {item}
                    </Typography>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Box>
      )}

      {/* ── Requirement Breakdown (collapsible) ──────────────────────── */}
      {requirements.length > 0 && (
        <Box sx={{ mb: 1 }}>
          <Button
            size="small"
            startIcon={showRequirements ? <ExpandLessRoundedIcon /> : <ExpandMoreRoundedIcon />}
            onClick={() => setShowRequirements((v) => !v)}
            sx={{ mb: 1 }}
          >
            {showRequirements ? 'Hide' : 'Show'} Requirement Breakdown ({requirements.length})
          </Button>
          <Collapse in={showRequirements}>
            <List dense disablePadding>
              {requirements.map((req, i) => {
                const statusColor =
                  req.status === 'COMPLETED' ? 'success' :
                  req.status === 'PARTIALLY_COMPLETED' ? 'warning' :
                  req.status === 'MISSING' ? 'error' : 'default';
                return (
                  <ListItem
                    key={i}
                    disableGutters
                    sx={{
                      py: 0.5,
                      borderBottom: '1px solid',
                      borderColor: 'divider',
                      flexDirection: 'column',
                      alignItems: 'flex-start',
                    }}
                  >
                    <Stack direction="row" spacing={1} alignItems="center" sx={{ width: '100%' }}>
                      <Chip
                        label={req.status?.replace('_', ' ') ?? 'UNCLEAR'}
                        color={statusColor}
                        size="small"
                        sx={{ fontSize: '0.65rem', height: 18 }}
                      />
                      <Typography variant="body2" fontWeight={500}>{req.requirement}</Typography>
                    </Stack>
                    {req.evidence && (
                      <Typography variant="caption" color="text.secondary" sx={{ mt: 0.25, ml: 0.5 }}>
                        Evidence: {req.evidence}
                      </Typography>
                    )}
                    {req.suggestion && req.status !== 'COMPLETED' && (
                      <Typography variant="caption" color="primary.main" sx={{ mt: 0.25, ml: 0.5 }}>
                        Suggestion: {req.suggestion}
                      </Typography>
                    )}
                  </ListItem>
                );
              })}
            </List>
          </Collapse>
        </Box>
      )}
    </Box>
  );
}

// ── History table ─────────────────────────────────────────────────────────────

function EvaluationHistoryTable({ reviews, onSelectReview, selectedReviewId }) {
  if (!reviews || reviews.length === 0) return null;

  return (
    <Box sx={{ mt: 3 }}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
        <HistoryRoundedIcon fontSize="small" color="action" />
        <Typography variant="subtitle2" fontWeight={700}>
          Evaluation History
        </Typography>
      </Stack>
      <Box sx={{ overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell align="center">Completion</TableCell>
              <TableCell align="center">Quality</TableCell>
              <TableCell>Recommendation</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {reviews.map((r) => (
              <TableRow
                key={r.id}
                hover
                selected={r.id === selectedReviewId}
                onClick={() => onSelectReview && onSelectReview(r)}
                sx={{ cursor: onSelectReview ? 'pointer' : 'default' }}
              >
                <TableCell>
                  <Typography variant="caption">{formatDate(r.completedAt || r.createdAt)}</Typography>
                </TableCell>
                <TableCell align="center">
                  <Typography variant="caption" fontWeight={600}>
                    {r.completionScore ?? '—'}
                  </Typography>
                </TableCell>
                <TableCell align="center">
                  <Typography variant="caption" fontWeight={600}>
                    {r.qualityScore ?? '—'}
                  </Typography>
                </TableCell>
                <TableCell>
                  {r.recommendedAction ? (
                    <RecommendationBadge action={r.recommendedAction} />
                  ) : (
                    <Typography variant="caption" color="text.secondary">—</Typography>
                  )}
                </TableCell>
                <TableCell>
                  <AiStatusChip status={r.status} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>
    </Box>
  );
}

// ── Main component ────────────────────────────────────────────────────────────

/**
 * AI Evaluation section for the manager task detail page.
 *
 * @param {{
 *   taskId: string,
 *   submission: import('../../services/taskSubmissionApi').TaskSubmissionResponse | null | undefined,
 *   onRunComplete?: (review: import('../../services/taskAiReviewApi').TaskAiReviewResponse) => void,
 * }} props
 */
export default function TaskAiEvaluationSection({ taskId, submission, onRunComplete }) {
  const submissionId = submission?.id ?? null;

  // ── Latest review ──────────────────────────────────────────────────────────
  const {
    data: latestReview,
    isLoading: isLoadingLatest,
    error: latestError,
  } = useLatestAiReview(submissionId, { enabled: Boolean(submissionId) });

  // ── History ────────────────────────────────────────────────────────────────
  const {
    data: allReviews,
    isLoading: isLoadingHistory,
  } = useAllAiReviews(submissionId, { enabled: Boolean(submissionId) });

  // ── Selected review for history detail ────────────────────────────────────
  const [selectedHistoryReview, setSelectedHistoryReview] = useState(null);

  // Displayed review: history selection overrides latest
  const displayedReview = selectedHistoryReview ?? latestReview;

  // ── Run mutation ───────────────────────────────────────────────────────────
  const runMutation = useRunAiReview();
  const [runError, setRunError] = useState(null);

  const handleRun = async () => {
    if (!submissionId) return;
    setRunError(null);
    setSelectedHistoryReview(null);
    try {
      const result = await runMutation.mutateAsync(submissionId);
      if (onRunComplete) onRunComplete(result);
    } catch (err) {
      const detail = err?.response?.data?.detail ?? 'Failed to start AI evaluation.';
      setRunError(detail);
    }
  };

  // ── No submission guard ────────────────────────────────────────────────────
  if (!submission) {
    return (
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
            <SmartToyRoundedIcon color="disabled" />
            <Typography variant="subtitle1" fontWeight={700} color="text.secondary">
              AI Evaluation
            </Typography>
          </Stack>
          <Alert severity="info">
            AI evaluation will become available after the employee submits the completed task.
          </Alert>
        </CardContent>
      </Card>
    );
  }

  // ── Determine if evaluation is in-flight ───────────────────────────────────
  const isInFlight = runMutation.isPending
    || latestReview?.status === 'PENDING'
    || latestReview?.status === 'PROCESSING';

  const hasCompleted = latestReview?.status === 'COMPLETED';
  const hasFailed = latestReview?.status === 'FAILED';
  const hasExisting = Boolean(latestReview);

  // No review yet (404 → latestError?.response?.status === 404 OR latestError is not 404)
  const noReviewYet = !latestReview && !isLoadingLatest
    && (!latestError || latestError?.response?.status === 404);

  // ── Run button label ───────────────────────────────────────────────────────
  const runButtonLabel = hasExisting
    ? isInFlight ? 'Evaluating…' : 'Re-run Evaluation'
    : 'Run AI Evaluation';

  const runButtonIcon = isInFlight
    ? <CircularProgress size={16} color="inherit" />
    : hasExisting
      ? <RefreshRoundedIcon />
      : <PlayArrowRoundedIcon />;

  return (
    <Card variant="outlined" sx={{ mb: 3, borderColor: 'primary.light', borderWidth: 1 }}>
      <CardContent>
        {/* ── Header ─────────────────────────────────────────────────── */}
        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }} flexWrap="wrap">
          <SmartToyRoundedIcon color="primary" />
          <Typography variant="subtitle1" fontWeight={700}>
            AI Evaluation
          </Typography>
          {latestReview && <AiStatusChip status={latestReview.status} />}
        </Stack>

        {/* ── Evaluation meta ─────────────────────────────────────────── */}
        {latestReview?.completedAt && (
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 2 }}>
            Evaluated: {formatDate(latestReview.completedAt)}
            {latestReview.requestedByName ? ` · by ${latestReview.requestedByName}` : ''}
            {latestReview.aiModel ? ` · model: ${latestReview.aiModel}` : ''}
          </Typography>
        )}

        {/* ── Loading skeleton ─────────────────────────────────────────── */}
        {(isLoadingLatest || isInFlight) && (
          <Box sx={{ mb: 2 }}>
            {isInFlight && !runMutation.isPending && (
              <Alert severity="info" icon={<HourglassTopRoundedIcon />} sx={{ mb: 2 }}>
                AI is evaluating this submission… This may take 10–30 seconds.
              </Alert>
            )}
            {(isLoadingLatest || (isInFlight && runMutation.isPending)) && (
              <Box>
                <Skeleton variant="text" width="60%" height={32} />
                <Skeleton variant="text" width="80%" />
                <Skeleton variant="rectangular" height={80} sx={{ mt: 1, borderRadius: 1 }} />
              </Box>
            )}
          </Box>
        )}

        {/* ── Error from run request ──────────────────────────────────── */}
        {runError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {runError}
          </Alert>
        )}

        {/* ── FAILED state ────────────────────────────────────────────── */}
        {hasFailed && displayedReview?.status === 'FAILED' && (
          <Alert
            severity="error"
            sx={{ mb: 2 }}
            action={
              <Button
                color="error"
                size="small"
                onClick={handleRun}
                disabled={runMutation.isPending}
              >
                Retry
              </Button>
            }
          >
            AI evaluation failed.
            {displayedReview.errorMessage ? ` Details: ${displayedReview.errorMessage}` : ''}
          </Alert>
        )}

        {/* ── No review yet ────────────────────────────────────────────── */}
        {noReviewYet && !isInFlight && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            No AI evaluation has been run for this submission yet.
          </Typography>
        )}

        {/* ── Completed evaluation result ──────────────────────────────── */}
        {displayedReview?.status === 'COMPLETED' && !isInFlight && (
          <>
            {selectedHistoryReview && (
              <Alert severity="info" sx={{ mb: 2 }}>
                Viewing historical evaluation from {formatDate(selectedHistoryReview.createdAt)}.{' '}
                <Button size="small" onClick={() => setSelectedHistoryReview(null)}>
                  View Latest
                </Button>
              </Alert>
            )}
            <EvaluationResult review={displayedReview} />
          </>
        )}

        <Divider sx={{ my: 2 }} />

        {/* ── Run button ──────────────────────────────────────────────── */}
        <Tooltip
          title={isInFlight ? 'Evaluation in progress…' : hasExisting ? 'Request a fresh AI evaluation' : 'Run the first AI evaluation for this submission'}
        >
          <span>
            <Button
              variant={hasExisting ? 'outlined' : 'contained'}
              color="primary"
              startIcon={runButtonIcon}
              onClick={handleRun}
              disabled={isInFlight || runMutation.isPending}
              size="small"
            >
              {runButtonLabel}
            </Button>
          </span>
        </Tooltip>

        {/* ── Evaluation history ──────────────────────────────────────── */}
        {allReviews && allReviews.length > 1 && (
          <EvaluationHistoryTable
            reviews={allReviews}
            selectedReviewId={selectedHistoryReview?.id ?? latestReview?.id}
            onSelectReview={(r) => setSelectedHistoryReview(r.id === latestReview?.id ? null : r)}
          />
        )}
      </CardContent>
    </Card>
  );
}
