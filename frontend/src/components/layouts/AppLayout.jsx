/**
 * @fileoverview AppLayout — authenticated application shell.
 *
 * Renders the light sidebar alongside the main content area.
 * The Topbar floats above the content. On mobile the sidebar is a temporary drawer.
 * On desktop the sidebar can be collapsed to icon-only mode.
 * Includes a floating AI Assistant button (bottom-right).
 */

import React, { useState, useCallback } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Box, Fab, Tooltip, useMediaQuery, useTheme } from '@mui/material';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import { ROUTES } from '@/constants/routes';

/** Expanded sidebar width in pixels. */
export const SIDEBAR_WIDTH = 256;

/** Collapsed (icon-only) sidebar width in pixels. */
export const SIDEBAR_COLLAPSED_WIDTH = 68;

/**
 * Authenticated application layout with sidebar + topbar.
 *
 * @returns {JSX.Element}
 */
export default function AppLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

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

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
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
          transition: 'margin-left 0.25s ease',
          minHeight: '100vh',
        }}
      >
        <Topbar
          onMenuClick={handleMobileToggle}
          onCollapseToggle={handleCollapseToggle}
          sidebarWidth={sidebarWidth}
          collapsed={collapsed}
        />

        {/* Page content — offset by topbar height (64px on desktop, 60px on mobile) */}
        <Box
          sx={{
            flexGrow: 1,
            p: { xs: 2, sm: 3 },
            pt: { xs: '76px', sm: '80px' },
            animation: 'fadeUp 0.3s ease',
          }}
        >
          <Outlet />
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
              background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
              color: '#fff',
              boxShadow: '0 8px 24px rgba(79,70,229,0.4)',
              '&:hover': {
                background: 'linear-gradient(135deg, #4338CA, #6D28D9)',
                boxShadow: '0 12px 32px rgba(79,70,229,0.5)',
                transform: 'scale(1.08)',
              },
              transition: 'all 0.2s ease',
              width: 52,
              height: 52,
            }}
          >
            <SmartToyRoundedIcon sx={{ fontSize: 22 }} />
          </Fab>
        </Tooltip>
      )}
    </Box>
  );
}
