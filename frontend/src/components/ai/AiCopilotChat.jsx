/**
 * @fileoverview AI Copilot chat component — Phase 7E.
 *
 * Upgrades the existing AI assistant into a controlled, role-aware agentic copilot.
 *
 * Features:
 * - Tool/action indicator showing which tools the agent executed
 * - Loading state with per-step status messages
 * - Confirmation card for proposed actions
 * - Clear distinction between information, recommendation, action proposal, and completed action
 * - Prompt injection: untrusted data is never treated as instructions
 */

import React, { useState, useRef, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  IconButton,
  CircularProgress,
  Alert,
  Tooltip,
  Divider,
  Stack,
  Chip,
  Button,
  Card,
  CardContent,
  CardActions,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Collapse,
} from '@mui/material';
import SendRoundedIcon from '@mui/icons-material/SendRounded';
import DeleteSweepRoundedIcon from '@mui/icons-material/DeleteSweepRounded';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import BuildIcon from '@mui/icons-material/Build';
import RecommendIcon from '@mui/icons-material/Recommend';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import TaskAltIcon from '@mui/icons-material/TaskAlt';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import { sendAgentMessage } from '@/services/agentApi';

/** @typedef {'INFORMATION'|'RECOMMENDATION'|'ACTION_PROPOSAL'|'ACTION_COMPLETED'|'ERROR'} ResponseType */

/**
 * @typedef {Object} AgentMessage
 * @property {'user'|'assistant'} role
 * @property {string} text
 * @property {ResponseType} [responseType]
 * @property {string[]} [toolsExecuted]
 * @property {string} [confirmationToken]
 * @property {string} [actionSummary]
 */

const MAX_MESSAGE_LENGTH = 4000;

const SUGGESTED_QUESTIONS = [
  'Who is overloaded on tasks right now?',
  'Which tasks are overdue?',
  'Who is on leave today?',
  'What are my current tasks?',
  'Show me pending leave requests.',
  'How many days in advance should I request remote work?',
];

/** Maps response type to color/icon */
function getResponseStyle(responseType) {
  switch (responseType) {
    case 'RECOMMENDATION':
      return { color: '#7c5cd8', icon: <RecommendIcon fontSize="small" /> };
    case 'ACTION_PROPOSAL':
      return { color: '#ed6c02', icon: <WarningAmberIcon fontSize="small" /> };
    case 'ACTION_COMPLETED':
      return { color: '#2e7d32', icon: <TaskAltIcon fontSize="small" /> };
    case 'ERROR':
      return { color: '#d32f2f', icon: <WarningAmberIcon fontSize="small" /> };
    default: // INFORMATION
      return { color: '#3b82d4', icon: <InfoOutlinedIcon fontSize="small" /> };
  }
}

/**
 * Small pill showing which tools were executed.
 */
function ToolsExecutedBadge({ tools }) {
  const [expanded, setExpanded] = useState(false);
  if (!tools || tools.length === 0) return null;

  const formatToolName = (name) => name.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());

  return (
    <Box sx={{ mt: 0.5, mb: 0.5 }}>
      <Stack direction="row" spacing={0.5} alignItems="center" flexWrap="wrap">
        <BuildIcon sx={{ fontSize: 12, color: 'text.disabled' }} />
        <Typography variant="caption" color="text.disabled">
          Tools used:
        </Typography>
        {tools.slice(0, expanded ? undefined : 2).map((t) => (
          <Chip
            key={t}
            label={formatToolName(t)}
            size="small"
            variant="outlined"
            sx={{ fontSize: '0.65rem', height: 18 }}
          />
        ))}
        {tools.length > 2 && (
          <Chip
            label={expanded ? 'less' : `+${tools.length - 2} more`}
            size="small"
            variant="outlined"
            onClick={() => setExpanded((x) => !x)}
            sx={{ fontSize: '0.65rem', height: 18, cursor: 'pointer' }}
          />
        )}
      </Stack>
    </Box>
  );
}

/**
 * Confirmation card shown when the agent proposes an action.
 */
function ConfirmationCard({ message, onConfirm, onDecline, loading }) {
  return (
    <Card variant="outlined" sx={{ borderColor: 'warning.main', borderWidth: 1.5, mx: 1, my: 0.5 }}>
      <CardContent sx={{ pb: 0.5 }}>
        <Stack direction="row" spacing={1} alignItems="flex-start" mb={1}>
          <WarningAmberIcon sx={{ color: 'warning.main', mt: 0.25, flexShrink: 0 }} />
          <Box>
            <Typography variant="subtitle2" fontWeight={700} color="warning.dark">
              Action Requires Confirmation
            </Typography>
            <Typography variant="body2" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>
              {message}
            </Typography>
          </Box>
        </Stack>
      </CardContent>
      <CardActions sx={{ pt: 0, gap: 1 }}>
        <Button
          variant="contained"
          color="warning"
          size="small"
          disabled={loading}
          onClick={onConfirm}
          startIcon={
            loading ? <CircularProgress size={14} color="inherit" /> : <CheckCircleOutlineIcon />
          }
        >
          {loading ? 'Processing…' : 'Confirm'}
        </Button>
        <Button
          variant="outlined"
          color="inherit"
          size="small"
          disabled={loading}
          onClick={onDecline}
        >
          Cancel
        </Button>
      </CardActions>
    </Card>
  );
}

/**
 * Individual message bubble for the agent chat.
 */
function AgentMessageBubble({ msg, onConfirm, onDecline, confirmLoading }) {
  const isUser = msg.role === 'user';
  const responseStyle = msg.responseType ? getResponseStyle(msg.responseType) : null;

  if (isUser) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'row-reverse', alignItems: 'flex-start', gap: 1 }}>
        <Box
          sx={{
            width: 30,
            height: 30,
            borderRadius: '50%',
            bgcolor: 'primary.main',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            mt: 0.25,
          }}
        >
          <PersonRoundedIcon sx={{ fontSize: 16, color: 'primary.contrastText' }} />
        </Box>
        <Paper
          elevation={0}
          sx={{
            px: 1.5,
            py: 1,
            maxWidth: '80%',
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
            borderRadius: 2,
            borderTopRightRadius: 0,
          }}
        >
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>
            {msg.text}
          </Typography>
        </Paper>
      </Box>
    );
  }

  // Assistant message
  return (
    <Box sx={{ display: 'flex', flexDirection: 'row', alignItems: 'flex-start', gap: 1 }}>
      <Box
        sx={{
          width: 30,
          height: 30,
          borderRadius: '50%',
          bgcolor: 'action.selected',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          mt: 0.25,
        }}
      >
        <SmartToyRoundedIcon sx={{ fontSize: 16 }} />
      </Box>

      <Box sx={{ flex: 1, maxWidth: 'calc(100% - 42px)' }}>
        {/* Response type badge */}
        {responseStyle && (
          <Stack direction="row" spacing={0.5} alignItems="center" mb={0.5}>
            <Box sx={{ color: responseStyle.color, display: 'flex' }}>{responseStyle.icon}</Box>
            <Typography variant="caption" sx={{ color: responseStyle.color, fontWeight: 600 }}>
              {msg.responseType?.replace('_', ' ')}
            </Typography>
          </Stack>
        )}

        {/* Action proposal: show confirmation card */}
        {msg.responseType === 'ACTION_PROPOSAL' ? (
          <ConfirmationCard
            message={msg.text}
            onConfirm={() => onConfirm(msg.confirmationToken)}
            onDecline={onDecline}
            loading={confirmLoading}
          />
        ) : (
          <Paper
            elevation={0}
            sx={{
              px: 1.5,
              py: 1,
              bgcolor: 'action.hover',
              color: 'text.primary',
              borderRadius: 2,
              borderTopLeftRadius: 0,
            }}
          >
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>
              {msg.text}
            </Typography>
          </Paper>
        )}

        {/* Tools executed */}
        <ToolsExecutedBadge tools={msg.toolsExecuted} />
      </Box>
    </Box>
  );
}

/**
 * AI Copilot chat component — Phase 7E.
 *
 * @returns {JSX.Element}
 */
export default function AiCopilotChat() {
  /** @type {[AgentMessage[], React.Dispatch<React.SetStateAction<AgentMessage[]>>]} */
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [error, setError] = useState('');
  const [loadingStatus, setLoadingStatus] = useState('');

  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const canSend = input.trim().length > 0 && input.length <= MAX_MESSAGE_LENGTH && !loading;

  const handleSend = async (textOverride) => {
    const text = (textOverride ?? input).trim();
    if (!text || loading) return;

    setError('');
    setInput('');
    setMessages((prev) => [...prev, { role: 'user', text }]);
    setLoading(true);
    setLoadingStatus('Thinking…');

    try {
      const response = await sendAgentMessage(text);
      setLoadingStatus('');

      // Show tool progress hint based on tools executed
      const toolsExecuted = response.toolsExecuted || [];

      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: response.answer,
          responseType: response.responseType,
          toolsExecuted,
          confirmationToken: response.confirmationToken,
          actionSummary: response.actionSummary,
        },
      ]);
    } catch (err) {
      setError(friendlyError(err));
    } finally {
      setLoading(false);
      setLoadingStatus('');
      setTimeout(() => inputRef.current?.focus(), 0);
    }
  };

  const handleConfirm = async (confirmationToken) => {
    if (!confirmationToken || confirmLoading) return;
    setConfirmLoading(true);
    setError('');

    try {
      // Send confirmation with a "Yes" message
      setMessages((prev) => [...prev, { role: 'user', text: 'Yes, proceed.' }]);
      const response = await sendAgentMessage('Yes, proceed.', confirmationToken);

      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: response.answer,
          responseType: response.responseType,
          toolsExecuted: response.toolsExecuted || [],
          actionSummary: response.actionSummary,
        },
      ]);
    } catch (err) {
      setError(friendlyError(err));
    } finally {
      setConfirmLoading(false);
    }
  };

  const handleDecline = () => {
    setMessages((prev) => [
      ...prev,
      { role: 'user', text: 'No, cancel.' },
      {
        role: 'assistant',
        text: 'Understood. The action has been cancelled.',
        responseType: 'INFORMATION',
      },
    ]);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (canSend) handleSend();
    }
  };

  const handleClear = () => {
    setMessages([]);
    setError('');
    setInput('');
    inputRef.current?.focus();
  };

  return (
    <Paper
      elevation={0}
      sx={{
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
        display: 'flex',
        flexDirection: 'column',
        height: { xs: '75vh', sm: '70vh' },
        minHeight: 480,
        maxHeight: 800,
        overflow: 'hidden',
      }}
    >
      {/* ── Header ──────────────────────────────────────────────────────── */}
      <Box
        sx={{
          px: 2,
          py: 1.5,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid',
          borderColor: 'divider',
          bgcolor: 'background.paper',
        }}
      >
        <Stack direction="row" spacing={1} alignItems="center">
          <SmartToyRoundedIcon sx={{ color: 'primary.main', fontSize: 22 }} />
          <Typography variant="subtitle1" fontWeight={600}>
            AI Copilot
          </Typography>
          <Chip
            label="Phase 7E"
            size="small"
            color="primary"
            variant="outlined"
            sx={{ fontSize: '0.65rem', height: 18 }}
          />
        </Stack>

        {messages.length > 0 && (
          <Tooltip title="Clear conversation">
            <IconButton size="small" onClick={handleClear} aria-label="Clear conversation">
              <DeleteSweepRoundedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
      </Box>

      {/* ── Message list ─────────────────────────────────────────────────── */}
      <Box
        sx={{
          flex: 1,
          overflowY: 'auto',
          px: 2,
          py: 2,
          display: 'flex',
          flexDirection: 'column',
          gap: 1.5,
        }}
      >
        {/* Empty state */}
        {messages.length === 0 && !loading && (
          <Box
            sx={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'text.secondary',
              textAlign: 'center',
              gap: 2,
              py: 4,
            }}
          >
            <SmartToyRoundedIcon sx={{ fontSize: 48, opacity: 0.25 }} />
            <Box>
              <Typography variant="h6" fontWeight={600} color="text.primary" gutterBottom>
                AI Copilot
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Ask me about your team, tasks, leaves, workload, or HR policies. I can look up live
                data and propose controlled actions.
              </Typography>
            </Box>
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 1,
                width: '100%',
                maxWidth: 480,
              }}
            >
              <Typography variant="caption" color="text.disabled" sx={{ mb: 0.5 }}>
                Try asking:
              </Typography>
              {SUGGESTED_QUESTIONS.map((q) => (
                <Chip
                  key={q}
                  label={q}
                  variant="outlined"
                  size="small"
                  onClick={() => handleSend(q)}
                  disabled={loading}
                  sx={{
                    cursor: 'pointer',
                    height: 'auto',
                    '& .MuiChip-label': {
                      whiteSpace: 'normal',
                      textAlign: 'center',
                      py: 0.75,
                      px: 1,
                    },
                    maxWidth: '100%',
                    fontSize: '0.75rem',
                  }}
                />
              ))}
            </Box>
          </Box>
        )}

        {/* Messages */}
        {messages.map((msg, idx) => (
          <AgentMessageBubble
            key={idx}
            msg={msg}
            onConfirm={handleConfirm}
            onDecline={handleDecline}
            confirmLoading={confirmLoading}
          />
        ))}

        {/* Loading */}
        {loading && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box
              sx={{
                width: 30,
                height: 30,
                borderRadius: '50%',
                bgcolor: 'action.selected',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <SmartToyRoundedIcon sx={{ fontSize: 16 }} />
            </Box>
            <Stack direction="row" spacing={0.5} alignItems="center">
              <CircularProgress size={14} />
              <Typography variant="caption" color="text.secondary">
                {loadingStatus || 'AI Copilot is working…'}
              </Typography>
            </Stack>
          </Box>
        )}

        <div ref={bottomRef} />
      </Box>

      {/* ── Error banner ─────────────────────────────────────────────────── */}
      {error && (
        <>
          <Divider />
          <Alert severity="error" onClose={() => setError('')} sx={{ borderRadius: 0, px: 2 }}>
            {error}
          </Alert>
        </>
      )}

      {/* ── Input ────────────────────────────────────────────────────────── */}
      <Divider />
      <Box
        sx={{
          px: 2,
          py: 1.5,
          display: 'flex',
          alignItems: 'flex-end',
          gap: 1,
          bgcolor: 'background.paper',
        }}
      >
        <TextField
          inputRef={inputRef}
          fullWidth
          multiline
          maxRows={4}
          size="small"
          placeholder="Ask a question or request an action… (Shift+Enter for new line)"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={loading}
          inputProps={{ 'aria-label': 'Message input', maxLength: MAX_MESSAGE_LENGTH + 1 }}
          helperText={
            input.length > MAX_MESSAGE_LENGTH
              ? `Too long (${input.length}/${MAX_MESSAGE_LENGTH})`
              : undefined
          }
          error={input.length > MAX_MESSAGE_LENGTH}
        />
        <Tooltip title={canSend ? 'Send (Enter)' : ''}>
          <span>
            <IconButton
              color="primary"
              onClick={() => handleSend()}
              disabled={!canSend}
              aria-label="Send message"
              sx={{ mb: input.length > MAX_MESSAGE_LENGTH ? 3 : 0 }}
            >
              {loading ? <CircularProgress size={20} color="inherit" /> : <SendRoundedIcon />}
            </IconButton>
          </span>
        </Tooltip>
      </Box>
    </Paper>
  );
}

function friendlyError(err) {
  if (!err) return 'An unexpected error occurred.';
  if (err.status === 0 || err.isNetwork) return 'Unable to reach the server.';
  if (err.status === 401) return 'Your session has expired. Please log in again.';
  if (err.status === 403) return 'You do not have permission to use the AI Copilot.';
  if (err.message) return err.message;
  return 'The AI Copilot is temporarily unavailable. Please try again later.';
}
