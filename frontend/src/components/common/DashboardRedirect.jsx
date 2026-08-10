/**
 * @fileoverview DashboardRedirect — smart redirect based on authenticated user role.
 *
 * Reads the authenticated user's roles from {@link AuthContext} and redirects
 * to the appropriate role-specific dashboard:
 *
 * | Role            | Target                  |
 * |-----------------|-------------------------|
 * | ROLE_ADMIN      | /admin/dashboard        |
 * | ROLE_HR         | /hr/dashboard           |
 * | ROLE_MANAGER    | /hr/dashboard           |
 * | ROLE_EMPLOYEE   | /employee/dashboard     |
 * | (unknown)       | /employee/dashboard     |
 *
 * Provides backward compatibility for any bookmark or link pointing to
 * the legacy `/dashboard` route.
 */

import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';
import { ROUTES } from '@/constants/routes';

/**
 * Resolves the correct dashboard path for the given role array.
 *
 * @param {string[]} roles
 * @returns {string}
 */
function resolveDashboardPath(roles = []) {
  if (roles.includes(ROLES.ADMIN))                           return ROUTES.ADMIN_DASHBOARD;
  if (roles.includes(ROLES.HR) || roles.includes(ROLES.MANAGER)) return ROUTES.HR_DASHBOARD;
  return ROUTES.EMPLOYEE_DASHBOARD;
}

/**
 * Immediately redirects to the role-appropriate dashboard.
 * Renders nothing visible — only a {@code <Navigate>} element.
 *
 * @returns {JSX.Element}
 */
export default function DashboardRedirect() {
  const { user } = useAuth();
  const target = resolveDashboardPath(user?.roles ?? []);
  return <Navigate to={target} replace />;
}
