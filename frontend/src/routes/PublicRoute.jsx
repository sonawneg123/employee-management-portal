/**
 * @fileoverview PublicRoute — redirects authenticated users away from auth pages.
 *
 * Prevents already-logged-in users from seeing /login or /register.
 * If authenticated, they are sent to /dashboard (or the redirect param).
 */

import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { ROUTES } from '@/constants/routes';
import LoadingScreen from '@/components/common/LoadingScreen';

/**
 * Route guard that keeps authenticated users away from public auth pages.
 *
 * Renders the nested {@code <Outlet />} when the user is NOT authenticated.
 * Redirects authenticated users to the dashboard (or the redirect param).
 *
 * @returns {JSX.Element}
 */
export default function PublicRoute() {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (isAuthenticated) {
    const params = new URLSearchParams(location.search);
    const redirect = params.get('redirect') ?? ROUTES.DASHBOARD;
    return <Navigate to={redirect} replace />;
  }

  return <Outlet />;
}
