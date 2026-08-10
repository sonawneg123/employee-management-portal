/**
 * @fileoverview Application root component.
 *
 * {@link App} is the composition root — it assembles every top-level provider
 * in the correct nesting order and mounts the route tree.
 *
 * Provider order (outermost → innermost):
 * 1. {@link HelmetProvider}      — react-helmet-async document head management
 * 2. {@link ThemeProvider}       — MUI theme + colour mode + CssBaseline
 * 3. {@link QueryClientProvider} — TanStack React Query global client
 * 4. {@link BrowserRouter}       — React Router v7 history context
 * 5. {@link AuthProvider}        — authentication state (requires Router for useNavigate)
 * 6. {@link ToastContainer}      — react-toastify notification stack
 * 7. {@link ErrorBoundary}       — catches unexpected render errors
 * 8. {@link AppRoutes}           — the full route tree
 */

import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { HelmetProvider } from 'react-helmet-async';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import { ThemeProvider } from '@/theme/ThemeContext';
import { AuthProvider } from '@/contexts/AuthContext';
import { queryClient } from '@/api/queryClient';
import AppRoutes from '@/routes/AppRoutes';
import ErrorBoundary from '@/components/common/ErrorBoundary';

/**
 * Root application component.
 *
 * @returns {JSX.Element}
 */
export default function App() {
  return (
    <HelmetProvider>
      <ThemeProvider>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <AuthProvider>
              <ErrorBoundary>
                <AppRoutes />
              </ErrorBoundary>

              {/* Toast notifications — rendered at the root level so they
                  appear above all content regardless of z-index stacking */}
              <ToastContainer
                position="top-right"
                autoClose={4000}
                hideProgressBar={false}
                newestOnTop
                closeOnClick
                pauseOnFocusLoss
                draggable
                pauseOnHover
                theme="colored"
              />
            </AuthProvider>
          </BrowserRouter>

          {/* TanStack Query Devtools — only visible in development builds */}
          {import.meta.env.DEV && (
            <ReactQueryDevtools initialIsOpen={false} position="bottom-right" />
          )}
        </QueryClientProvider>
      </ThemeProvider>
    </HelmetProvider>
  );
}
