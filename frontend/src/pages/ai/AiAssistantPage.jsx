/**
 * @fileoverview AI Copilot page — Phase 7E.
 *
 * Replaces the Phase 1 general HR assistant with the full Agentic AI Copilot.
 * The original simple RAG assistant is preserved and accessible via the Legacy tab.
 */

import React, { useState } from 'react';
import { Box, Typography, Alert, Tabs, Tab } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import AiCopilotChat from '@/components/ai/AiCopilotChat';
import AiAssistantChat from '@/components/ai/AiAssistantChat';

/**
 * AI Copilot page.
 *
 * @returns {JSX.Element}
 */
export default function AiAssistantPage() {
  const [tab, setTab] = useState(0);

  return (
    <>
      <Helmet>
        <title>AI Copilot — Employee Management Portal</title>
      </Helmet>

      <Box sx={{ maxWidth: 880, mx: 'auto', px: { xs: 1, sm: 2 }, py: 3 }}>
        {/* Page header */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
          <SmartToyRoundedIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Box>
            <Typography variant="h5" fontWeight={700}>
              AI Copilot
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Powered by Groq · Phase 7E — Agentic
            </Typography>
          </Box>
        </Box>

        {/* Tabs */}
        <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Tab label="AI Copilot (Agentic)" />
          <Tab label="General HR Assistant (Legacy)" />
        </Tabs>

        {tab === 0 && (
          <>
            <Alert severity="info" sx={{ mb: 2 }}>
              <strong>Phase 7E — Agentic AI Copilot.</strong> I have access to live application data
              and can perform controlled actions with your confirmation. My tool access is scoped to
              your role — I cannot access data outside your permissions.
            </Alert>
            <AiCopilotChat />
          </>
        )}

        {tab === 1 && (
          <>
            <Alert severity="info" sx={{ mb: 2 }}>
              <strong>General HR Guidance Only.</strong> This assistant provides general HR information
              and company policy guidance. It does not have access to live application data.
            </Alert>
            <AiAssistantChat />
          </>
        )}
      </Box>
    </>
  );
}
