/**
 * @fileoverview AI Assistant page.
 *
 * A thin page wrapper around {@link AiAssistantChat} that provides the page
 * title, header, and the disclaimer about Phase 1 limitations.
 */

import React from 'react';
import { Box, Typography, Alert } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import AiAssistantChat from '@/components/ai/AiAssistantChat';

/**
 * AI Assistant page component.
 *
 * @returns {JSX.Element}
 */
export default function AiAssistantPage() {
  return (
    <>
      <Helmet>
        <title>AI Assistant — Employee Management Portal</title>
      </Helmet>

      <Box sx={{ maxWidth: 800, mx: 'auto', px: { xs: 1, sm: 2 }, py: 3 }}>
        {/* ── Page header ──────────────────────────────────────────────── */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
          <SmartToyRoundedIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Box>
            <Typography variant="h5" fontWeight={700}>
              HR AI Assistant
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Powered by Groq · Phase 1
            </Typography>
          </Box>
        </Box>

        {/* ── Phase 1 disclaimer ────────────────────────────────────────── */}
        <Alert severity="info" sx={{ mb: 2 }}>
          <strong>Phase 1 — General HR Guidance Only.</strong> The AI assistant provides general HR
          information and guidance. It does not have access to your personal records, leave balances,
          or any private company data in this phase.
        </Alert>

        {/* ── Chat UI ──────────────────────────────────────────────────── */}
        <AiAssistantChat />
      </Box>
    </>
  );
}
