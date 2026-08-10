/**
 * @fileoverview RoleProtectedRoute — extends ProtectedRoute with role-level access control.
 *
 * Renders the nested {@code <Outlet />} only when the authenticated user
 * possesses at least one of the required roles. Otherwise redirects to the
 * Access Denied page.
 *
 * Usage:
 * ```jsx
 * <Route element={<RoleProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
 *   <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
 * </Route>
 * ```
 */

import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { ROUTES } from '@/constants/routes';
import LoadingScreen from '@/components/common/LoadingScreen';

/**
 * Route guard that enforces both authentication and role membership.
 *
 * Must be nested inside a {@link ProtectedRoute} (or any component that
 * already guarantees the user is authenticated). If used standalone, it
 * also handles the unauthenticated redirect.
 *
 * @param {{
 *   allowedRoles: string[],
 * }} props
 * @returns {JSX.Element}
 */
export default function RoleProtectedRoute({ allowedRoles }) {
  const { isAuthenticated, isLoading, hasAnyRole } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} replace />;
  }

  if (!hasAnyRole(allowedRoles)) {
    return <Navigate to={ROUTES.ACCESS_DENIED} replace />;
  }

  return <Outlet />;
}
