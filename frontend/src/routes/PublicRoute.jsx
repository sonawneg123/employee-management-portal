/**
 * @fileoverview PublicRoute — redirects authenticated users away from auth pages.
 *
 * Prevents already-logged-in users from seeing /login or /register.
 * If authenticated, they are sent to /dashboard (or the redirect param).
 *
 * NOTE: isLoading from AuthContext is intentionally NOT used here.
 * AuthContext.isLoading becomes true while a login mutation is in flight.
 * Gating on isLoading would unmount the <Outlet /> (LoginForm) during
 * submission, destroying its mutation error state so the "Invalid email or
 * password." message can never be displayed after a failed attempt.
 */

import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { ROUTES } from '@/constants/routes';

/**
 * Route guard that keeps authenticated users away from public auth pages.
 *
 * Renders the nested {@code <Outlet />} when the user is NOT authenticated.
 * Redirects authenticated users to the dashboard (or the redirect param).
 *
 * @returns {JSX.Element}
 */
export default function PublicRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (isAuthenticated) {
    const params = new URLSearchParams(location.search);
    const redirect = params.get('redirect') ?? ROUTES.DASHBOARD;
    return <Navigate to={redirect} replace />;
  }

  return <Outlet />;
}
