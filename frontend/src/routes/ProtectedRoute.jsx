/**
 * @fileoverview ProtectedRoute — redirects unauthenticated users to /login.
 *
 * Wrap any route that requires the user to be authenticated with this component.
 * The current path is stored in the redirect query param so that the login
 * page can return the user to their intended destination after login.
 */

import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { ROUTES } from '@/constants/routes';
import LoadingScreen from '@/components/common/LoadingScreen';

/**
 * Route guard that enforces authentication.
 *
 * Renders the nested {@code <Outlet />} when the user is authenticated.
 * Redirects to {@code /login?redirect=<currentPath>} otherwise.
 *
 * @returns {JSX.Element}
 */
export default function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated) {
    return (
      <Navigate
        to={`${ROUTES.LOGIN}?redirect=${encodeURIComponent(location.pathname)}`}
        replace
      />
    );
  }

  return <Outlet />;
}
