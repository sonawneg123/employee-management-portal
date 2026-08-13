/**
 * @fileoverview AppLayout — authenticated application shell.
 *
 * Renders the dark persistent Sidebar alongside the main content area.
 * The Topbar floats above the content. On mobile the sidebar is a temporary drawer.
 * On desktop the sidebar can be collapsed to icon-only mode.
 */

import React, { useState, useCallback } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, useMediaQuery, useTheme } from '@mui/material';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

/** Expanded sidebar width in pixels. */
export const SIDEBAR_WIDTH = 256;

/** Collapsed (icon-only) sidebar width in pixels. */
export const SIDEBAR_COLLAPSED_WIDTH = 64;

/**
 * Authenticated application layout with dark sidebar + topbar.
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
        }}
      >
        <Topbar
          onMenuClick={handleMobileToggle}
          onCollapseToggle={handleCollapseToggle}
          sidebarWidth={sidebarWidth}
          collapsed={collapsed}
        />

        {/* Page content — offset by topbar height */}
        <Box
          sx={{
            flexGrow: 1,
            p: { xs: 2, sm: 3 },
            pt: { xs: 9, sm: 10 },
            animation: 'fadeUp 0.3s ease',
          }}
        >
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
