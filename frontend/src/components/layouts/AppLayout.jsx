/**
 * @fileoverview AppLayout — authenticated application shell.
 *
 * Renders the dark persistent Sidebar alongside the main content area.
 * The Topbar floats above the content. On mobile the sidebar is a temporary drawer.
 */

import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, useMediaQuery, useTheme } from '@mui/material';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

/** Sidebar width in pixels. */
const SIDEBAR_WIDTH = 256;

/**
 * Authenticated application layout with dark sidebar + topbar.
 *
 * @returns {JSX.Element}
 */
export default function AppLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      {/* Sidebar */}
      <Sidebar
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        width={SIDEBAR_WIDTH}
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
          ml: isMobile ? 0 : `${SIDEBAR_WIDTH}px`,
        }}
      >
        <Topbar onMenuClick={() => setSidebarOpen((prev) => !prev)} sidebarWidth={SIDEBAR_WIDTH} />

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
