/**
 * @fileoverview AppLayout — premium authenticated application shell.
 *
 * A large rounded content area sits on a warm cream background with subtle
 * atmospheric gradients, modelled on a premium SaaS HR product design.
 *
 * Desktop layout: sidebar + main content area inside a rounded container.
 * Mobile layout: drawer sidebar, single column content.
 * Includes floating AI Assistant button (bottom-right).
 */

import React, { useState, useCallback } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Box, Fab, Tooltip, useMediaQuery, useTheme } from '@mui/material';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import { ROUTES } from '@/constants/routes';

/** Expanded sidebar width in pixels. */
export const SIDEBAR_WIDTH = 248;

/** Collapsed (icon-only) sidebar width in pixels. */
export const SIDEBAR_COLLAPSED_WIDTH = 68;

/**
 * Authenticated application layout — premium SaaS shell.
 *
 * @returns {JSX.Element}
 */
export default function AppLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const isDark = theme.palette.mode === 'dark';

  // Mobile: drawer open state
  const [mobileOpen, setMobileOpen] = useState(false);
  // Desktop: collapsed state
  const [collapsed, setCollapsed] = useState(false);

  const handleMobileToggle = useCallback(() => setMobileOpen((prev) => !prev), []);
  const handleCollapseToggle = useCallback(() => setCollapsed((prev) => !prev), []);
  const handleMobileClose = useCallback(() => setMobileOpen(false), []);

  const sidebarWidth = isMobile
    ? SIDEBAR_WIDTH
    : collapsed
      ? SIDEBAR_COLLAPSED_WIDTH
      : SIDEBAR_WIDTH;

  const navigate = useNavigate();
  const location = useLocation();
  const isAiPage = location.pathname === ROUTES.AI_ASSISTANT;

  // Background: warm cream with very subtle blue/purple atmospheric gradients
  const pageBg = isDark
    ? 'radial-gradient(ellipse at 10% 20%, rgba(26,35,66,0.8) 0%, transparent 60%), radial-gradient(ellipse at 90% 80%, rgba(20,28,55,0.6) 0%, transparent 60%), #0C1220'
    : 'radial-gradient(ellipse at 10% 0%, rgba(210,215,255,0.25) 0%, transparent 50%), radial-gradient(ellipse at 90% 100%, rgba(255,240,200,0.3) 0%, transparent 50%), #F5F0E8';

  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100vh',
        background: pageBg,
        position: 'relative',
      }}
    >
      {/* Sidebar */}
      <Sidebar
        open={mobileOpen}
        onClose={handleMobileClose}
        onCollapseToggle={handleCollapseToggle}
        width={SIDEBAR_WIDTH}
        collapsedWidth={SIDEBAR_COLLAPSED_WIDTH}
        collapsed={collapsed}
        variant={isMobile ? 'temporary' : 'permanent'}
      />

      {/* Main content area */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          display: 'flex',
          flexDirection: 'column',
          minWidth: 0,
          ml: isMobile ? 0 : `${sidebarWidth}px`,
          transition: 'margin-left 0.28s cubic-bezier(0.4,0,0.2,1)',
          minHeight: '100vh',
        }}
      >
        <Topbar
          onMenuClick={handleMobileToggle}
          onCollapseToggle={handleCollapseToggle}
          sidebarWidth={sidebarWidth}
          collapsed={collapsed}
        />

        {/* Page content — inside a rounded container with spacious padding */}
        <Box
          sx={{
            flexGrow: 1,
            p: { xs: 2, sm: 3 },
            pt: { xs: '76px', sm: '84px' },
          }}
        >
          {/* Inner rounded container — the premium SaaS "workspace" feel */}
          <Box
            sx={{
              minHeight: 'calc(100vh - 100px)',
              bgcolor: isDark ? 'rgba(19,28,46,0.6)' : 'rgba(255,255,255,0.55)',
              borderRadius: { xs: 3, sm: 4 },
              border: `1px solid ${isDark ? 'rgba(240,237,230,0.06)' : 'rgba(235,230,218,0.7)'}`,
              backdropFilter: 'blur(12px)',
              boxShadow: isDark
                ? '0 4px 32px rgba(0,0,0,0.25), inset 0 1px 0 rgba(255,255,255,0.03)'
                : '0 4px 32px rgba(26,35,66,0.06), inset 0 1px 0 rgba(255,255,255,0.9)',
              p: { xs: 2, sm: 3 },
              animation: 'fadeUp 0.3s ease',
            }}
          >
            <Outlet />
          </Box>
        </Box>
      </Box>

      {/* Floating AI Assistant button — bottom-right, hidden on AI page */}
      {!isAiPage && (
        <Tooltip title="Open AI Copilot" placement="left">
          <Fab
            onClick={() => navigate(ROUTES.AI_ASSISTANT)}
            aria-label="Open AI Copilot"
            size="medium"
            sx={{
              position: 'fixed',
              bottom: { xs: 20, sm: 28 },
              right: { xs: 20, sm: 28 },
              zIndex: 1200,
              background: isDark
                ? 'linear-gradient(135deg, #2D3A6B, #4F6AB5)'
                : 'linear-gradient(135deg, #1A2342, #2D3A6B)',
              color: '#F5C518',
              boxShadow: isDark
                ? '0 8px 28px rgba(26,35,66,0.6), 0 0 0 0 rgba(245,197,24,0.15)'
                : '0 8px 28px rgba(26,35,66,0.35), 0 0 0 0 rgba(245,197,24,0.15)',
              animation: 'aiFabPulse 3s ease-in-out infinite',
              '@keyframes aiFabPulse': {
                '0%, 100%': {
                  boxShadow: isDark
                    ? '0 8px 28px rgba(26,35,66,0.6), 0 0 0 0 rgba(245,197,24,0.15)'
                    : '0 8px 28px rgba(26,35,66,0.35), 0 0 0 0 rgba(245,197,24,0.15)',
                },
                '50%': {
                  boxShadow: isDark
                    ? '0 8px 28px rgba(26,35,66,0.6), 0 0 0 8px rgba(245,197,24,0)'
                    : '0 8px 28px rgba(26,35,66,0.35), 0 0 0 8px rgba(245,197,24,0)',
                },
              },
              '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
              '&:hover': {
                background: isDark
                  ? 'linear-gradient(135deg, #374D80, #5C7AC8)'
                  : 'linear-gradient(135deg, #2D3A6B, #1A2342)',
                boxShadow: isDark
                  ? '0 12px 40px rgba(26,35,66,0.8)'
                  : '0 12px 40px rgba(26,35,66,0.5)',
                transform: 'scale(1.08) translateY(-2px)',
                animation: 'none',
              },
              transition:
                'background 0.22s ease, box-shadow 0.22s ease, transform 0.22s cubic-bezier(0.4,0,0.2,1)',
              width: 54,
              height: 54,
              border: `2px solid ${isDark ? 'rgba(245,197,24,0.35)' : 'rgba(245,197,24,0.45)'}`,
            }}
          >
            <AutoAwesomeRoundedIcon sx={{ fontSize: 22 }} />
          </Fab>
        </Tooltip>
      )}
    </Box>
  );
}
