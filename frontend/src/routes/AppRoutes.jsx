/**
 * @fileoverview Route configuration for the Employee Management Portal.
 *
 * Uses React Router v7 with lazy-loaded page components wrapped in
 * {@code React.Suspense} so that each route chunk is only downloaded
 * when the user navigates to it.
 *
 * Route guards:
 * - {@link ProtectedRoute} — redirects unauthenticated users to /login.
 * - {@link PublicRoute}    — redirects authenticated users to /dashboard.
 */

import React, { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import ProtectedRoute from './ProtectedRoute';
import PublicRoute from './PublicRoute';
import PageLoader from '@/components/common/PageLoader';
import AppLayout from '@/components/layouts/AppLayout';

// ── Lazy page imports ────────────────────────────────────────────────────────
const LoginPage       = lazy(() => import('@/pages/auth/LoginPage'));
const RegisterPage    = lazy(() => import('@/pages/auth/RegisterPage'));
const DashboardPage   = lazy(() => import('@/pages/dashboard/DashboardPage'));
const EmployeesPage        = lazy(() => import('@/pages/employees/EmployeesPage'));
const EmployeeDetailsPage  = lazy(() => import('@/pages/employees/EmployeeDetailsPage'));
const DepartmentsPage       = lazy(() => import('@/pages/departments/DepartmentsPage'));
const DepartmentDetailsPage = lazy(() => import('@/pages/departments/DepartmentDetailsPage'));
const LeavesPage         = lazy(() => import('@/pages/leaves/LeavesPage'));
const LeaveDetailsPage   = lazy(() => import('@/pages/leaves/LeaveDetailsPage'));
const MyLeavesPage       = lazy(() => import('@/pages/leaves/MyLeavesPage'));
const AttendancePage     = lazy(() => import('@/pages/attendance/AttendancePage'));
const ReviewsPage     = lazy(() => import('@/pages/reviews/ReviewsPage'));
const ProfilePage     = lazy(() => import('@/pages/profile/ProfilePage'));
const SettingsPage    = lazy(() => import('@/pages/settings/SettingsPage'));
const NotFoundPage    = lazy(() => import('@/pages/NotFoundPage'));
const AccessDeniedPage = lazy(() => import('@/pages/AccessDeniedPage'));

// ── Suspense wrapper ─────────────────────────────────────────────────────────

/**
 * Wraps a lazy component with a full-screen {@link PageLoader} Suspense fallback.
 *
 * @param {JSX.Element} element - The lazy-loaded element to wrap.
 * @returns {JSX.Element}
 */
function withSuspense(element) {
  return <Suspense fallback={<PageLoader />}>{element}</Suspense>;
}

/**
 * The root route tree for the application.
 *
 * @returns {JSX.Element}
 */
export default function AppRoutes() {
  return (
    <Routes>
      {/* ── Public routes (redirect to dashboard if already logged in) ── */}
      <Route element={<PublicRoute />}>
        <Route path={ROUTES.LOGIN}    element={withSuspense(<LoginPage />)} />
        <Route path={ROUTES.REGISTER} element={withSuspense(<RegisterPage />)} />
      </Route>

      {/* ── Protected routes (redirect to login if not authenticated) ── */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to={ROUTES.DASHBOARD} replace />} />
          <Route path={ROUTES.DASHBOARD}   element={withSuspense(<DashboardPage />)} />
          <Route path={ROUTES.EMPLOYEES}          element={withSuspense(<EmployeesPage />)} />
          <Route path={`${ROUTES.EMPLOYEES}/:id`} element={withSuspense(<EmployeeDetailsPage />)} />
          <Route path={ROUTES.DEPARTMENTS}          element={withSuspense(<DepartmentsPage />)} />
          <Route path={`${ROUTES.DEPARTMENTS}/:id`} element={withSuspense(<DepartmentDetailsPage />)} />
          <Route path={ROUTES.LEAVES}                         element={withSuspense(<LeavesPage />)} />
          <Route path={ROUTES.MY_LEAVES}                      element={withSuspense(<MyLeavesPage />)} />
          <Route path={`${ROUTES.LEAVES}/:id`}                element={withSuspense(<LeaveDetailsPage />)} />
          <Route path={ROUTES.ATTENDANCE}  element={withSuspense(<AttendancePage />)} />
          <Route path={ROUTES.REVIEWS}     element={withSuspense(<ReviewsPage />)} />
          <Route path={ROUTES.PROFILE}     element={withSuspense(<ProfilePage />)} />
          <Route path={ROUTES.SETTINGS}    element={withSuspense(<SettingsPage />)} />
        </Route>
      </Route>

      {/* ── Error pages ── */}
      <Route path={ROUTES.ACCESS_DENIED} element={withSuspense(<AccessDeniedPage />)} />
      <Route path={ROUTES.NOT_FOUND}     element={withSuspense(<NotFoundPage />)} />
      <Route path="*"                    element={<Navigate to={ROUTES.NOT_FOUND} replace />} />
    </Routes>
  );
}
