/**
 * @fileoverview AI Copilot page — premium SaaS redesign.
 *
 * Replaces the Phase 1 general HR assistant with the full Agentic AI Copilot.
 * The original simple RAG assistant is preserved and accessible via the Legacy tab.
 * Premium SaaS design — navy + gold accent.
 */

import React, { useState } from 'react';
import { Box, Typography, Alert, Tabs, Tab, Chip, useTheme } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import SupportAgentRoundedIcon from '@mui/icons-material/SupportAgentRounded';
import AiCopilotChat from '@/components/ai/AiCopilotChat';
import AiAssistantChat from '@/components/ai/AiAssistantChat';

/**
 * AI Copilot page — premium SaaS design.
 *
 * @returns {JSX.Element}
 */
export default function AiAssistantPage() {
  const [tab, setTab] = useState(0);
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  return (
    <>
      <Helmet>
        <title>AI Copilot — PeopleCore HR</title>
      </Helmet>

      <Box sx={{ maxWidth: 960, mx: 'auto', pb: 4 }}>
        {/* Page header — navy with gold accents */}
        <Box
          sx={{
            mb: 3,
            borderRadius: '24px',
            background: 'linear-gradient(135deg, #0F1628 0%, #1A2342 50%, #2D3A6B 100%)',
            p: { xs: 3, sm: 3.5 },
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 2,
            position: 'relative',
            overflow: 'hidden',
            boxShadow: '0 8px 40px rgba(26,35,66,0.35)',
            '&::before': {
              content: '""',
              position: 'absolute',
              top: -60,
              right: -60,
              width: 200,
              height: 200,
              borderRadius: '50%',
              background: 'radial-gradient(circle, rgba(245,197,24,0.12) 0%, transparent 70%)',
              pointerEvents: 'none',
            },
            '&::after': {
              content: '""',
              position: 'absolute',
              bottom: -80,
              left: 20,
              width: 200,
              height: 200,
              borderRadius: '50%',
              background: 'radial-gradient(circle, rgba(79,106,181,0.18) 0%, transparent 70%)',
              pointerEvents: 'none',
            },
          }}
        >
          <Box
            sx={{ display: 'flex', alignItems: 'center', gap: 2, position: 'relative', zIndex: 1 }}
          >
            {/* Gold icon container */}
            <Box
              sx={{
                width: 52,
                height: 52,
                borderRadius: '16px',
                background: 'linear-gradient(135deg, rgba(245,197,24,0.25), rgba(245,197,24,0.1))',
                border: '1px solid rgba(245,197,24,0.3)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
              aria-hidden="true"
            >
              <AutoAwesomeRoundedIcon sx={{ fontSize: 26, color: '#F5C518' }} />
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
              <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.55)' }}>
                Powered by Groq · Agentic HR Intelligence
              </Typography>
            </Box>
          </Box>
          <Chip
            label="Agentic · Role-scoped"
            size="small"
            sx={{
              bgcolor: 'rgba(245,197,24,0.15)',
              color: '#F5C518',
              border: '1px solid rgba(245,197,24,0.3)',
              fontWeight: 600,
              fontSize: '0.75rem',
              position: 'relative',
              zIndex: 1,
            }}
          />
        </Box>

        {/* Chat container */}
        <Box
          sx={{
            bgcolor: isDark ? '#131C2E' : '#FFFFFF',
            borderRadius: '20px',
            border: `1px solid ${isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA'}`,
            overflow: 'hidden',
            boxShadow: isDark ? '0 4px 24px rgba(0,0,0,0.3)' : '0 4px 24px rgba(26,35,66,0.07)',
          }}
        >
          {/* Tab navigation */}
          <Box
            sx={{
              px: 3,
              pt: 2,
              borderBottom: `1px solid ${isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA'}`,
              bgcolor: isDark ? 'rgba(255,255,255,0.02)' : '#FAF7F0',
            }}
          >
            <Tabs
              value={tab}
              onChange={(_, v) => setTab(v)}
              sx={{
                minHeight: 44,
                '& .MuiTabs-indicator': {
                  backgroundColor: '#F5C518',
                  height: 2,
                },
              }}
            >
              <Tab
                icon={<AutoAwesomeRoundedIcon sx={{ fontSize: 16 }} />}
                iconPosition="start"
                label="AI Copilot"
                sx={{
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  gap: 0.75,
                  color: isDark ? 'rgba(240,237,230,0.55)' : '#7A7468',
                  '&.Mui-selected': {
                    color: isDark ? '#F5C518' : '#1A2342',
                  },
                }}
              />
              <Tab
                icon={<SupportAgentRoundedIcon sx={{ fontSize: 16 }} />}
                iconPosition="start"
                label="HR Assistant"
                sx={{
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  gap: 0.75,
                  color: isDark ? 'rgba(240,237,230,0.55)' : '#7A7468',
                  '&.Mui-selected': {
                    color: isDark ? '#F5C518' : '#1A2342',
                  },
                }}
              />
            </Tabs>
          </Box>

          <Box sx={{ p: { xs: 2, sm: 3 } }}>
            {tab === 0 && (
              <>
                <Alert
                  severity="info"
                  icon={<AutoAwesomeRoundedIcon />}
                  sx={{
                    mb: 3,
                    borderRadius: '12px',
                    bgcolor: isDark ? 'rgba(245,197,24,0.06)' : 'rgba(245,197,24,0.06)',
                    border: `1px solid rgba(245,197,24,0.2)`,
                    color: isDark ? '#F5C518' : '#92700A',
                    '& .MuiAlert-icon': {
                      color: '#F5C518',
                    },
                  }}
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
                  sx={{
                    mb: 3,
                    borderRadius: '12px',
                    bgcolor: isDark ? 'rgba(59,130,246,0.06)' : 'rgba(59,130,246,0.04)',
                    border: '1px solid rgba(59,130,246,0.2)',
                  }}
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
