/**
 * @fileoverview AI HR Assistant chat interface.
 *
 * A minimal, self-contained chat UI that:
 * - Accepts a message from the user
 * - Sends it to POST /api/ai/chat via the backend (not directly to Groq)
 * - Displays the AI response, loading state, and errors
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
} from '@mui/material';
import SendRoundedIcon from '@mui/icons-material/SendRounded';
import DeleteSweepRoundedIcon from '@mui/icons-material/DeleteSweepRounded';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import { sendAiMessage } from '@/services/aiApi';

/** @typedef {{ role: 'user' | 'assistant'; text: string }} ChatMessage */

const MAX_MESSAGE_LENGTH = 4000;

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
  }
  // Message from the error object
  if (err.message) return err.message;
  return 'The AI assistant is temporarily unavailable. Please try again later.';
}

/**
 * AI HR Assistant chat component.
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
        height: '70vh',
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
            (Beta · Phase 1)
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
              gap: 1,
              py: 4,
            }}
          >
            <SmartToyRoundedIcon sx={{ fontSize: 48, opacity: 0.25 }} />
            <Typography variant="body2" color="text.secondary">
              Ask me anything about HR policies, leave, attendance, or general workplace guidance.
            </Typography>
            <Typography variant="caption" color="text.disabled">
              I do not have access to private employee records in this phase.
            </Typography>
          </Box>
        )}

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

        {/* Loading indicator */}
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
            <CircularProgress size={16} />
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
          placeholder="Ask a HR question… (Shift+Enter for new line)"
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
