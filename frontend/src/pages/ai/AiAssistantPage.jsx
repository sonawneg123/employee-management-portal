/**
 * @fileoverview AI Copilot page — Phase 7E.
 *
 * Replaces the Phase 1 general HR assistant with the full Agentic AI Copilot.
 * The original simple RAG assistant is preserved and accessible via the Legacy tab.
 */

import React, { useState } from 'react';
import { Box, Typography, Alert, Tabs, Tab, Chip } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import SupportAgentRoundedIcon from '@mui/icons-material/SupportAgentRounded';
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
        <title>AI Copilot — PeopleCore HR</title>
      </Helmet>

      <Box sx={{ maxWidth: 920, mx: 'auto', pb: 4 }}>
        {/* Page header */}
        <Box
          sx={{
            mb: 3,
            borderRadius: '20px',
            background: 'linear-gradient(135deg, #243B7A 0%, #4F46E5 60%, #7C3AED 100%)',
            p: { xs: 3, sm: 3.5 },
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 2,
            position: 'relative',
            overflow: 'hidden',
            boxShadow: '0 8px 32px rgba(36,59,122,0.25)',
            '&::before': {
              content: '""',
              position: 'absolute',
              top: -40,
              right: -40,
              width: 160,
              height: 160,
              borderRadius: '50%',
              background: 'rgba(255,255,255,0.06)',
              pointerEvents: 'none',
            },
          }}
        >
          <Box
            sx={{ display: 'flex', alignItems: 'center', gap: 2, position: 'relative', zIndex: 1 }}
          >
            <Box
              sx={{
                width: 48,
                height: 48,
                borderRadius: '14px',
                bgcolor: 'rgba(255,255,255,0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
              aria-hidden="true"
            >
              <SmartToyRoundedIcon sx={{ fontSize: 26, color: '#fff' }} />
            </Box>
            <Box>
              <Typography
                variant="h4"
                fontWeight={800}
                sx={{
                  color: '#FFFFFF',
                  letterSpacing: '-0.02em',
                  lineHeight: 1.2,
                  fontSize: { xs: '1.25rem', sm: '1.5rem' },
                }}
              >
                AI Copilot
              </Typography>
              <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)' }}>
                Powered by Groq · Agentic HR Intelligence
              </Typography>
            </Box>
          </Box>
          <Chip
            label="Phase 7E · Agentic"
            size="small"
            sx={{
              bgcolor: 'rgba(255,255,255,0.15)',
              color: '#fff',
              border: '1px solid rgba(255,255,255,0.3)',
              fontWeight: 600,
              fontSize: '0.75rem',
              position: 'relative',
              zIndex: 1,
            }}
          />
        </Box>

        {/* Tabs */}
        <Box
          sx={{
            bgcolor: 'background.paper',
            borderRadius: '16px',
            border: '1px solid',
            borderColor: 'divider',
            overflow: 'hidden',
          }}
        >
          <Box
            sx={{
              px: 3,
              pt: 2,
              borderBottom: '1px solid',
              borderColor: 'divider',
            }}
          >
            <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ minHeight: 44 }}>
              <Tab
                icon={<AutoAwesomeRoundedIcon sx={{ fontSize: 16 }} />}
                iconPosition="start"
                label="AI Copilot"
                sx={{ fontWeight: 600, fontSize: '0.875rem', gap: 0.75 }}
              />
              <Tab
                icon={<SupportAgentRoundedIcon sx={{ fontSize: 16 }} />}
                iconPosition="start"
                label="HR Assistant"
                sx={{ fontWeight: 600, fontSize: '0.875rem', gap: 0.75 }}
              />
            </Tabs>
          </Box>

          <Box sx={{ p: { xs: 2, sm: 3 } }}>
            {tab === 0 && (
              <>
                <Alert
                  severity="info"
                  icon={<AutoAwesomeRoundedIcon />}
                  sx={{ mb: 3, borderRadius: '12px' }}
                >
                  <strong>Agentic AI Copilot.</strong> I have access to live application data and
                  can perform controlled actions with your confirmation. My tool access is scoped to
                  your role — I cannot access data outside your permissions.
                </Alert>
                <AiCopilotChat />
              </>
            )}

            {tab === 1 && (
              <>
                <Alert
                  severity="info"
                  icon={<SupportAgentRoundedIcon />}
                  sx={{ mb: 3, borderRadius: '12px' }}
                >
                  <strong>General HR Guidance Only.</strong> This assistant provides general HR
                  information and company policy guidance. It does not have access to live
                  application data.
                </Alert>
                <AiAssistantChat />
              </>
            )}
          </Box>
        </Box>
      </Box>
    </>
  );
}
