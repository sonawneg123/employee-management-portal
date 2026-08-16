/**
 * @fileoverview AI HR Assistant chat interface.
 *
 * A self-contained chat UI that:
 * - Accepts a message from the user
 * - Sends it to POST /api/ai/chat via the backend (not directly to Groq)
 * - Displays the AI response, loading state, and errors
 * - Shows an empty state with suggested HR questions
 * - Allows clearing the conversation
 *
 * Consistent with the existing MUI-based design system.
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
} from '@mui/material';
import SendRoundedIcon from '@mui/icons-material/SendRounded';
import DeleteSweepRoundedIcon from '@mui/icons-material/DeleteSweepRounded';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import { sendAiMessage } from '@/services/aiApi';

/** @typedef {{ role: 'user' | 'assistant'; text: string }} ChatMessage */

const MAX_MESSAGE_LENGTH = 4000;

/**
 * Suggested HR questions shown in the empty state.
 * Clicking one populates the input and sends immediately.
 *
 * @type {string[]}
 */
const SUGGESTED_QUESTIONS = [
  'How many days in advance should I request remote work?',
  'Does my manager need to approve remote work?',
  'When can remote work be denied?',
  'What is the annual leave entitlement?',
  'How do I submit a leave request?',
];

/**
 * Formats a raw API error into a friendly display string.
 *
 * @param {import('@/api/axiosInstance').NormalisedError | Error | unknown} err
 * @returns {string}
 */
function friendlyError(err) {
  if (!err) return 'An unexpected error occurred. Please try again.';
  // Normalised axios error
  if (err.status !== undefined) {
    if (err.status === 0 || err.isNetwork) {
      return 'Unable to reach the server. Please check your connection.';
    }
    if (err.status === 401) {
      return 'Your session has expired. Please log in again.';
    }
    if (err.status === 403) {
      return 'You do not have permission to use the AI Assistant.';
    }
  }
  // Message from the error object
  if (err.message) return err.message;
  return 'The AI assistant is temporarily unavailable. Please try again later.';
}

/**
 * AI HR Assistant chat component.
 *
 * Renders the full chat interface including:
 * - Empty state with suggested questions
 * - Message bubbles for user and assistant turns
 * - Loading indicator while awaiting a response
 * - Error banner for API/network failures
 * - Textarea input with Enter-to-send
 *
 * @returns {JSX.Element}
 */
export default function AiAssistantChat() {
  /** @type {[ChatMessage[], React.Dispatch<React.SetStateAction<ChatMessage[]>>]} */
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const canSend = input.trim().length > 0 && input.length <= MAX_MESSAGE_LENGTH && !loading;

  /**
   * Sends the current input value as a user message and awaits the AI response.
   *
   * @returns {Promise<void>}
   */
  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return;

    setError('');
    setInput('');
    setMessages((prev) => [...prev, { role: 'user', text }]);
    setLoading(true);

    try {
      const response = await sendAiMessage(text);
      setMessages((prev) => [...prev, { role: 'assistant', text: response.answer }]);
    } catch (err) {
      setError(friendlyError(err));
    } finally {
      setLoading(false);
      // Refocus input for keyboard convenience
      setTimeout(() => inputRef.current?.focus(), 0);
    }
  };

  /**
   * Handles Enter-to-send (Shift+Enter inserts a newline).
   *
   * @param {React.KeyboardEvent<HTMLDivElement>} e
   */
  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (canSend) handleSend();
    }
  };

  /** Clears all messages and resets the input. */
  const handleClear = () => {
    setMessages([]);
    setError('');
    setInput('');
    inputRef.current?.focus();
  };

  /**
   * Populates the input with a suggested question and sends it immediately.
   *
   * @param {string} question
   */
  const handleSuggestedQuestion = (question) => {
    if (loading) return;
    setError('');
    setInput('');
    setMessages((prev) => [...prev, { role: 'user', text: question }]);
    setLoading(true);

    sendAiMessage(question)
      .then((response) => {
        setMessages((prev) => [...prev, { role: 'assistant', text: response.answer }]);
      })
      .catch((err) => {
        setError(friendlyError(err));
      })
      .finally(() => {
        setLoading(false);
        setTimeout(() => inputRef.current?.focus(), 0);
      });
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
        minHeight: 420,
        maxHeight: 720,
        overflow: 'hidden',
      }}
    >
      {/* ── Header ─────────────────────────────────────────────────────────── */}
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
            HR AI Assistant
          </Typography>
          <Typography variant="caption" color="text.secondary">
            (Beta)
          </Typography>
        </Stack>

        {messages.length > 0 && (
          <Tooltip title="Clear conversation">
            <IconButton
              size="small"
              onClick={handleClear}
              aria-label="Clear conversation"
            >
              <DeleteSweepRoundedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
      </Box>

      {/* ── Message list ───────────────────────────────────────────────────── */}
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
        {/* ── Empty state ─────────────────────────────────────────────────── */}
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
                How can I help you?
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Ask me about HR policies, leave, remote work, or workplace procedures.
              </Typography>
            </Box>

            {/* ── Suggested questions ───────────────────────────────────── */}
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 1,
                width: '100%',
                maxWidth: 480,
                mt: 1,
              }}
            >
              <Typography variant="caption" color="text.disabled" sx={{ mb: 0.5 }}>
                Try asking:
              </Typography>
              {SUGGESTED_QUESTIONS.map((question) => (
                <Chip
                  key={question}
                  label={question}
                  variant="outlined"
                  size="small"
                  onClick={() => handleSuggestedQuestion(question)}
                  disabled={loading}
                  aria-label={`Suggested question: ${question}`}
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

        {/* ── Messages ────────────────────────────────────────────────────── */}
        {messages.map((msg, idx) => (
          <Box
            key={idx}
            sx={{
              display: 'flex',
              flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
              alignItems: 'flex-start',
              gap: 1,
            }}
          >
            {/* Avatar icon */}
            <Box
              sx={{
                width: 30,
                height: 30,
                borderRadius: '50%',
                bgcolor: msg.role === 'user' ? 'primary.main' : 'action.selected',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                mt: 0.25,
              }}
            >
              {msg.role === 'user' ? (
                <PersonRoundedIcon sx={{ fontSize: 16, color: 'primary.contrastText' }} />
              ) : (
                <SmartToyRoundedIcon sx={{ fontSize: 16 }} />
              )}
            </Box>

            {/* Bubble */}
            <Paper
              elevation={0}
              sx={{
                px: 1.5,
                py: 1,
                maxWidth: '80%',
                bgcolor:
                  msg.role === 'user' ? 'primary.main' : 'action.hover',
                color: msg.role === 'user' ? 'primary.contrastText' : 'text.primary',
                borderRadius: 2,
                borderTopRightRadius: msg.role === 'user' ? 0 : 2,
                borderTopLeftRadius: msg.role === 'assistant' ? 0 : 2,
              }}
            >
              <Typography
                variant="body2"
                sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}
              >
                {msg.text}
              </Typography>
            </Paper>
          </Box>
        ))}

        {/* ── Loading indicator ────────────────────────────────────────────── */}
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
                AI Assistant is thinking…
              </Typography>
            </Stack>
          </Box>
        )}

        {/* Scroll anchor */}
        <div ref={bottomRef} />
      </Box>

      {/* ── Error banner ───────────────────────────────────────────────────── */}
      {error && (
        <>
          <Divider />
          <Alert
            severity="error"
            onClose={() => setError('')}
            sx={{ borderRadius: 0, px: 2 }}
          >
            {error}
          </Alert>
        </>
      )}

      {/* ── Input area ─────────────────────────────────────────────────────── */}
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
          placeholder="Ask an HR question… (Shift+Enter for new line)"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={loading}
          inputProps={{
            'aria-label': 'Message input',
            maxLength: MAX_MESSAGE_LENGTH + 1, // allow over-length so validation can catch it
          }}
          helperText={
            input.length > MAX_MESSAGE_LENGTH
              ? `Message too long (${input.length}/${MAX_MESSAGE_LENGTH})`
              : undefined
          }
          error={input.length > MAX_MESSAGE_LENGTH}
        />
        <Tooltip title={canSend ? 'Send message (Enter)' : ''}>
          <span>
            <IconButton
              color="primary"
              onClick={handleSend}
              disabled={!canSend}
              aria-label="Send message"
              sx={{ mb: input.length > MAX_MESSAGE_LENGTH ? 3 : 0 }}
            >
              {loading ? (
                <CircularProgress size={20} color="inherit" />
              ) : (
                <SendRoundedIcon />
              )}
            </IconButton>
          </span>
        </Tooltip>
      </Box>
    </Paper>
  );
}
