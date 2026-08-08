/**
 * @fileoverview AppLayout — the authenticated application shell.
 *
 * Renders the persistent Sidebar and Topbar alongside the page content area.
 * The {@link Outlet} from React Router renders the active route's page component.
 */

import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Box, useMediaQuery, useTheme } from '@mui/material';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

/** Width of the sidebar when open (pixels). */
const SIDEBAR_WIDTH = 260;

/**
 * The main authenticated application layout.
 *
 * On mobile (< md breakpoint) the sidebar is rendered as a temporary drawer.
 * On desktop it is persistent and always visible.
 *
 * @returns {JSX.Element}
 */
export default function AppLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);

  /** Toggles the sidebar open/closed state. */
  const handleSidebarToggle = () => setSidebarOpen((prev) => !prev);

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
          transition: theme.transitions.create('margin', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
        }}
      >
        <Topbar onMenuClick={handleSidebarToggle} sidebarWidth={SIDEBAR_WIDTH} />

        {/* Page content */}
        <Box
          sx={{
            flexGrow: 1,
            p: { xs: 2, sm: 3 },
            pt: { xs: 10, sm: 11 },
          }}
        >
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
