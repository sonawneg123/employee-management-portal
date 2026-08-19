/**
 * @fileoverview EmployeeAiFeedbackSection — employee-safe AI feedback display.
 *
 * Phase 7D: Shows employees a safe summary of their completed AI evaluation.
 *
 * WHAT IS SHOWN:
 *   - Overall score, work quality, completeness, relevance
 *   - Short AI-generated summary
 *   - Strengths
 *   - Areas to improve
 *   - Suggestions for the next submission
 *   - Evaluation timestamp
 *   - "How was this evaluated?" explanation
 *
 * WHAT IS NEVER SHOWN:
 *   - Manager-only recommendation (recommendedAction)
 *   - Internal manager notes (managerSummary)
 *   - Raw AI response or structured JSON
 *   - Error details / stack traces
 *   - AI provider internals (API keys, model)
 *   - Other employees' data
 *
 * The AI evaluation is advisory only. Manager decisions always take precedence.
 */

import React, { useState } from 'react';
import {
  Alert,
  Box,
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
  Typography,
} from '@mui/material';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import TrendingUpRoundedIcon from '@mui/icons-material/TrendingUpRounded';
import LightbulbOutlinedIcon from '@mui/icons-material/LightbulbOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded';
import ExpandLessRoundedIcon from '@mui/icons-material/ExpandLessRounded';
import HourglassEmptyRoundedIcon from '@mui/icons-material/HourglassEmptyRounded';
import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import { useEmployeeAiFeedback, useEmployeeAiHistory } from '@/hooks/useTaskAiReviewHooks';

// ── Helpers ────────────────────────────────────────────────────────────────────

function formatDate(isoString) {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function ScoreBar({ label, value, color = 'primary' }) {
  if (value == null) return null;
  const pct = Math.max(0, Math.min(100, value));
  let barColor = color;
  if (pct >= 75) barColor = 'success';
  else if (pct >= 50) barColor = 'warning';
  else barColor = 'error';

  return (
    <Box sx={{ mb: 1.5 }}>
      <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
        <Typography variant="caption" color="text.secondary">
          {label}
        </Typography>
        <Typography variant="caption" fontWeight={700}>
          {pct}%
        </Typography>
      </Stack>
      <LinearProgress
        variant="determinate"
        value={pct}
        color={barColor}
        sx={{ height: 6, borderRadius: 3 }}
      />
    </Box>
  );
}

function AiStatusChip({ status }) {
  const configs = {
    PENDING: {
      label: 'In Queue',
      color: 'default',
      icon: <HourglassEmptyRoundedIcon fontSize="small" />,
    },
    PROCESSING: { label: 'Generating…', color: 'info', icon: <CircularProgress size={12} /> },
    COMPLETED: {
      label: 'Completed',
      color: 'success',
      icon: <CheckCircleOutlineRoundedIcon fontSize="small" />,
    },
    FAILED: {
      label: 'Unavailable',
      color: 'error',
      icon: <ErrorOutlineRoundedIcon fontSize="small" />,
    },
  };
  const cfg = configs[status] || configs.PENDING;
  return <Chip icon={cfg.icon} label={cfg.label} color={cfg.color} size="small" />;
}

// ── AI Evaluation History Table ───────────────────────────────────────────────

function AiFeedbackHistoryTable({ history, onSelect, selectedId }) {
  if (!history?.length) return null;

  return (
    <Box sx={{ mt: 2 }}>
      <Typography variant="subtitle2" gutterBottom color="text.secondary">
        Evaluation History
      </Typography>
      <Box
        component="table"
        sx={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem' }}
        data-testid="ai-feedback-history-table"
      >
        <Box component="thead">
          <Box component="tr">
            {['#', 'Date', 'Score', 'Status'].map((h) => (
              <Box
                key={h}
                component="th"
                sx={{
                  textAlign: 'left',
                  p: '4px 8px',
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                  color: 'text.secondary',
                  fontWeight: 600,
                  fontSize: '0.75rem',
                }}
              >
                {h}
              </Box>
            ))}
          </Box>
        </Box>
        <Box component="tbody">
          {history.map((item, idx) => {
            const isSelected = item.id === selectedId;
            return (
              <Box
                key={item.id}
                component="tr"
                data-testid={`ai-history-row-${idx}`}
                onClick={() => onSelect(item.id === selectedId ? null : item.id)}
                sx={{
                  cursor: 'pointer',
                  bgcolor: isSelected ? 'action.selected' : 'transparent',
                  '&:hover': { bgcolor: 'action.hover' },
                  transition: 'background 0.1s',
                }}
              >
                <Box component="td" sx={{ p: '4px 8px' }}>
                  {history.length - idx}
                </Box>
                <Box component="td" sx={{ p: '4px 8px' }}>
                  {item.evaluatedAt ? formatDate(item.evaluatedAt) : formatDate(item.requestedAt)}
                </Box>
                <Box component="td" sx={{ p: '4px 8px' }}>
                  {item.overallScore != null ? `${item.overallScore}%` : '—'}
                </Box>
                <Box component="td" sx={{ p: '4px 8px' }}>
                  <AiStatusChip status={item.status} />
                </Box>
              </Box>
            );
          })}
        </Box>
      </Box>
    </Box>
  );
}

// ── Completed feedback display ────────────────────────────────────────────────

function CompletedFeedback({ feedback }) {
  const [showExplanation, setShowExplanation] = useState(false);

  return (
    <Stack spacing={2}>
      {/* Scores */}
      {(feedback.overallScore != null || feedback.workQualityScore != null) && (
        <Box>
          <Typography variant="subtitle2" gutterBottom>
            Scores
          </Typography>
          <ScoreBar label="Overall Score" value={feedback.overallScore} />
          <ScoreBar label="Work Quality" value={feedback.workQualityScore} />
          <ScoreBar label="Completeness" value={feedback.completenessScore} />
          {feedback.relevanceScore != null && (
            <ScoreBar label="Confidence" value={feedback.relevanceScore} />
          )}
        </Box>
      )}

      <Divider />

      {/* Summary */}
      {feedback.summary && (
        <Box>
          <Typography variant="subtitle2" gutterBottom>
            AI Assessment
          </Typography>
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
            {feedback.summary}
          </Typography>
        </Box>
      )}

      {/* Strengths */}
      {feedback.strengths?.length > 0 && (
        <Box>
          <Typography variant="subtitle2" gutterBottom sx={{ color: 'success.main' }}>
            <CheckCircleOutlineRoundedIcon
              fontSize="small"
              sx={{ mr: 0.5, verticalAlign: 'middle' }}
            />
            Strengths
          </Typography>
          <List dense disablePadding>
            {feedback.strengths.map((s, i) => (
              <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                <ListItemIcon sx={{ minWidth: 24 }}>
                  <CheckCircleOutlineRoundedIcon fontSize="small" color="success" />
                </ListItemIcon>
                <ListItemText primary={s} primaryTypographyProps={{ variant: 'body2' }} />
              </ListItem>
            ))}
          </List>
        </Box>
      )}

      {/* Areas to improve */}
      {feedback.areasToImprove?.length > 0 && (
        <Box>
          <Typography variant="subtitle2" gutterBottom sx={{ color: 'warning.main' }}>
            <TrendingUpRoundedIcon fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
            Areas to Improve
          </Typography>
          <List dense disablePadding>
            {feedback.areasToImprove.map((a, i) => (
              <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                <ListItemIcon sx={{ minWidth: 24 }}>
                  <TrendingUpRoundedIcon fontSize="small" color="warning" />
                </ListItemIcon>
                <ListItemText primary={a} primaryTypographyProps={{ variant: 'body2' }} />
              </ListItem>
            ))}
          </List>
        </Box>
      )}

      {/* Suggestions for next submission */}
      {feedback.suggestionsForNextSubmission?.length > 0 && (
        <Box>
          <Typography variant="subtitle2" gutterBottom sx={{ color: 'info.main' }}>
            <LightbulbOutlinedIcon fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
            Suggestions for Next Submission
          </Typography>
          <List dense disablePadding>
            {feedback.suggestionsForNextSubmission.map((s, i) => (
              <ListItem key={i} disableGutters sx={{ py: 0.25 }}>
                <ListItemIcon sx={{ minWidth: 24 }}>
                  <LightbulbOutlinedIcon fontSize="small" color="info" />
                </ListItemIcon>
                <ListItemText primary={s} primaryTypographyProps={{ variant: 'body2' }} />
              </ListItem>
            ))}
          </List>
        </Box>
      )}

      {feedback.evaluatedAt && (
        <Typography variant="caption" color="text.secondary">
          Evaluated on {formatDate(feedback.evaluatedAt)}
        </Typography>
      )}

      {/* Advisory notice */}
      <Alert severity="info" icon={<InfoOutlinedIcon />} variant="outlined">
        <Typography variant="caption">
          <strong>AI evaluation is advisory only.</strong> Your manager makes the final decision.
        </Typography>
      </Alert>

      {/* How was this evaluated? */}
      {feedback.evaluationExplanation && (
        <Box>
          <Box
            component="button"
            onClick={() => setShowExplanation((v) => !v)}
            data-testid="show-explanation-btn"
            sx={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 0.5,
              color: 'text.secondary',
              p: 0,
              '&:hover': { color: 'text.primary' },
            }}
          >
            {showExplanation ? (
              <ExpandLessRoundedIcon fontSize="small" />
            ) : (
              <ExpandMoreRoundedIcon fontSize="small" />
            )}
            <Typography variant="caption">How was this evaluated?</Typography>
          </Box>
          <Collapse in={showExplanation}>
            <Box
              sx={{
                mt: 1,
                p: 1.5,
                bgcolor: 'action.hover',
                borderRadius: 1,
                border: '1px solid',
                borderColor: 'divider',
              }}
            >
              <Typography variant="caption" sx={{ whiteSpace: 'pre-wrap' }}>
                {feedback.evaluationExplanation}
              </Typography>
            </Box>
          </Collapse>
        </Box>
      )}
    </Stack>
  );
}

// ── Main component ─────────────────────────────────────────────────────────────

/**
 * Employee-facing AI feedback section shown on EmployeeTaskDetailPage.
 *
 * @param {{
 *   submissionId: string | null | undefined,
 *   taskId: string | null | undefined,
 * }} props
 */
export default function EmployeeAiFeedbackSection({ submissionId, taskId: _taskId }) {
  const [selectedHistoryId, setSelectedHistoryId] = useState(null);

  const {
    data: latestFeedback,
    isLoading,
    error: feedbackError,
  } = useEmployeeAiFeedback(submissionId);

  const { data: history } = useEmployeeAiHistory(submissionId, { enabled: Boolean(submissionId) });

  // When user selects a history item, show that item's feedback
  const selectedHistoryFeedback = selectedHistoryId
    ? (history?.find((h) => h.id === selectedHistoryId) ?? null)
    : null;

  const displayedFeedback = selectedHistoryFeedback ?? latestFeedback;

  if (!submissionId) {
    return null;
  }

  if (isLoading) {
    return (
      <Card variant="outlined" sx={{ mt: 2 }}>
        <CardContent>
          <Skeleton variant="text" width="60%" height={28} />
          <Skeleton variant="rectangular" height={80} sx={{ mt: 1 }} />
        </CardContent>
      </Card>
    );
  }

  // 404 — no AI review yet
  const is404 = feedbackError?.response?.status === 404;

  return (
    <Card variant="outlined" sx={{ mt: 2 }} data-testid="employee-ai-feedback-section">
      <CardContent>
        {/* Header */}
        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
          <AutoAwesomeRoundedIcon color="primary" fontSize="small" />
          <Typography variant="subtitle1" fontWeight={700}>
            AI Evaluation Feedback
          </Typography>
          {displayedFeedback && <AiStatusChip status={displayedFeedback.status} />}
        </Stack>

        <Divider sx={{ mb: 2 }} />

        {/* No AI review yet */}
        {is404 && (
          <Alert severity="info" data-testid="no-ai-feedback-yet">
            AI evaluation is in progress. Results will appear here automatically once completed.
          </Alert>
        )}

        {/* Other errors */}
        {feedbackError && !is404 && (
          <Alert severity="warning">
            AI feedback is temporarily unavailable. Please try again later.
          </Alert>
        )}

        {/* Pending */}
        {displayedFeedback?.status === 'PENDING' && (
          <Alert
            severity="info"
            icon={<HourglassEmptyRoundedIcon />}
            data-testid="ai-feedback-pending"
          >
            <Typography variant="body2" fontWeight={600}>
              AI evaluation in queue
            </Typography>
            <Typography variant="body2">
              Your submission is being queued for AI analysis. Results will appear here
              automatically.
            </Typography>
          </Alert>
        )}

        {/* Processing */}
        {displayedFeedback?.status === 'PROCESSING' && (
          <Alert
            severity="info"
            icon={<CircularProgress size={16} />}
            data-testid="ai-feedback-processing"
          >
            <Typography variant="body2" fontWeight={600}>
              AI evaluation in progress
            </Typography>
            <Typography variant="body2">
              The AI is evaluating your submission. This usually takes under a minute.
            </Typography>
          </Alert>
        )}

        {/* Completed */}
        {displayedFeedback?.status === 'COMPLETED' && (
          <CompletedFeedback feedback={displayedFeedback} />
        )}

        {/* Failed */}
        {displayedFeedback?.status === 'FAILED' && (
          <Alert severity="warning" data-testid="ai-feedback-failed">
            <Typography variant="body2" fontWeight={600}>
              AI evaluation unavailable
            </Typography>
            <Typography variant="body2">
              The AI evaluation could not be completed at this time. Your manager has been notified.
            </Typography>
          </Alert>
        )}

        {/* Evaluation history */}
        {history && history.length > 1 && (
          <AiFeedbackHistoryTable
            history={history}
            onSelect={setSelectedHistoryId}
            selectedId={selectedHistoryId}
          />
        )}
      </CardContent>
    </Card>
  );
}
