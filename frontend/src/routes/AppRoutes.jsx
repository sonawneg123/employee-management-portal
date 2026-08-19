/**
 * @fileoverview Route configuration for the Employee Management Portal.
 *
 * Uses React Router v7 with lazy-loaded page components wrapped in
 * {@code React.Suspense} so that each route chunk is only downloaded
 * when the user navigates to it.
 *
 * Route guards:
 * - {@link ProtectedRoute}     — redirects unauthenticated users to /login.
 * - {@link PublicRoute}        — redirects authenticated users to /dashboard.
 * - {@link RoleProtectedRoute} — redirects unauthorised roles to /403.
 *
 * Route tree:
 * - /admin/*    — ROLE_ADMIN only
 * - /hr/*       — ROLE_HR, ROLE_MANAGER
 * - /employee/* — ROLE_EMPLOYEE
 * - /dashboard  — smart redirect to the correct role dashboard
 * - Legacy flat routes remain for backward compatibility
 */

import React, { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import { ROLES } from '@/constants/roles';
import ProtectedRoute from './ProtectedRoute';
import PublicRoute from './PublicRoute';
import RoleProtectedRoute from './RoleProtectedRoute';
import PageLoader from '@/components/common/PageLoader';
import AppLayout from '@/components/layouts/AppLayout';
import DashboardRedirect from '@/components/common/DashboardRedirect';

// ── Lazy page imports ────────────────────────────────────────────────────────
const LandingPage = lazy(() => import('@/pages/LandingPage'));
const LoginPage = lazy(() => import('@/pages/auth/LoginPage'));
const RegisterPage = lazy(() => import('@/pages/auth/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('@/pages/auth/ForgotPasswordPage'));
const EmployeesPage = lazy(() => import('@/pages/employees/EmployeesPage'));
const EmployeeDetailsPage = lazy(() => import('@/pages/employees/EmployeeDetailsPage'));
const DepartmentsPage = lazy(() => import('@/pages/departments/DepartmentsPage'));
const DepartmentDetailsPage = lazy(() => import('@/pages/departments/DepartmentDetailsPage'));
const LeavesPage = lazy(() => import('@/pages/leaves/LeavesPage'));
const LeaveDetailsPage = lazy(() => import('@/pages/leaves/LeaveDetailsPage'));
const MyLeavesPage = lazy(() => import('@/pages/leaves/MyLeavesPage'));
const AttendancePage = lazy(() => import('@/pages/attendance/AttendancePage'));
const ReviewsPage = lazy(() => import('@/pages/reviews/ReviewsPage'));
const ProfilePage = lazy(() => import('@/pages/profile/ProfilePage'));
const SettingsPage = lazy(() => import('@/pages/settings/SettingsPage'));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'));
const AccessDeniedPage = lazy(() => import('@/pages/AccessDeniedPage'));

// ── Role-specific dashboard pages ────────────────────────────────────────────
const AdminDashboardPage = lazy(() => import('@/pages/admin/AdminDashboardPage'));
const AdminUsersPage = lazy(() => import('@/pages/admin/AdminUsersPage'));
const HRDashboardPage = lazy(() => import('@/pages/hr/HRDashboardPage'));
const EmployeeDashboardPage = lazy(() => import('@/pages/employee/EmployeeDashboardPage'));

// ── AI Assistant page ─────────────────────────────────────────────────────────
const AiAssistantPage = lazy(() => import('@/pages/ai/AiAssistantPage'));

// ── Company Policies page (Admin + HR) ────────────────────────────────────────
const CompanyPoliciesPage = lazy(() => import('@/pages/admin/CompanyPoliciesPage'));

// ── Task Management pages ─────────────────────────────────────────────────────
const ManagerTasksPage = lazy(() => import('@/pages/tasks/ManagerTasksPage'));
const ManagerTaskDetailPage = lazy(() => import('@/pages/tasks/ManagerTaskDetailPage'));
const ManagerTaskReviewsPage = lazy(() => import('@/pages/tasks/ManagerTasksPage')); // reuse list page filtered to reviews
const EmployeeTasksPage = lazy(() => import('@/pages/tasks/EmployeeTasksPage'));
const EmployeeTaskDetailPage = lazy(() => import('@/pages/tasks/EmployeeTaskDetailPage'));

// ── Manager Leave Approvals page ──────────────────────────────────────────────
const ManagerLeavePage = lazy(() => import('@/pages/leaves/ManagerLeavePage'));

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
      {/* ── Landing page (always public, no auth redirect) ─────────────── */}
      <Route path="/" element={withSuspense(<LandingPage />)} />

      {/* ── Public routes (redirect to dashboard if already logged in) ── */}
      <Route element={<PublicRoute />}>
        <Route path={ROUTES.LOGIN} element={withSuspense(<LoginPage />)} />
        <Route path={ROUTES.REGISTER} element={withSuspense(<RegisterPage />)} />
        <Route path={ROUTES.FORGOT_PASSWORD} element={withSuspense(<ForgotPasswordPage />)} />
        {/* Role-specific login routes */}
        <Route
          path={ROUTES.LOGIN_ADMIN}
          element={withSuspense(<LoginPage targetRole="ROLE_ADMIN" />)}
        />
        <Route path={ROUTES.LOGIN_HR} element={withSuspense(<LoginPage targetRole="ROLE_HR" />)} />
        <Route
          path={ROUTES.LOGIN_EMPLOYEE}
          element={withSuspense(<LoginPage targetRole="ROLE_EMPLOYEE" />)}
        />
        {/* Role-specific register routes */}
        <Route
          path={ROUTES.REGISTER_HR}
          element={withSuspense(<RegisterPage defaultRole="ROLE_HR" />)}
        />
        <Route
          path={ROUTES.REGISTER_EMPLOYEE}
          element={withSuspense(<RegisterPage defaultRole="ROLE_EMPLOYEE" />)}
        />
      </Route>

      {/* ── Protected routes (redirect to login if not authenticated) ── */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          {/* /dashboard → smart redirect based on role */}

          {/* Legacy /dashboard → smart redirect */}
          <Route path={ROUTES.DASHBOARD} element={<DashboardRedirect />} />

          {/* ── Admin-only routes (/admin/*) ──────────────────────────── */}
          <Route element={<RoleProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
            <Route path={ROUTES.ADMIN_DASHBOARD} element={withSuspense(<AdminDashboardPage />)} />
            <Route path={ROUTES.ADMIN_EMPLOYEES} element={withSuspense(<EmployeesPage />)} />
            <Route
              path={`${ROUTES.ADMIN_EMPLOYEES}/:id`}
              element={withSuspense(<EmployeeDetailsPage />)}
            />
            <Route path={ROUTES.ADMIN_DEPARTMENTS} element={withSuspense(<DepartmentsPage />)} />
            <Route
              path={`${ROUTES.ADMIN_DEPARTMENTS}/:id`}
              element={withSuspense(<DepartmentDetailsPage />)}
            />
            <Route path={ROUTES.ADMIN_LEAVES} element={withSuspense(<LeavesPage />)} />
            <Route path={ROUTES.ADMIN_ATTENDANCE} element={withSuspense(<AttendancePage />)} />
            <Route path={ROUTES.ADMIN_REVIEWS} element={withSuspense(<ReviewsPage />)} />
            <Route path={ROUTES.ADMIN_USERS} element={withSuspense(<AdminUsersPage />)} />
            <Route path={ROUTES.ADMIN_POLICIES} element={withSuspense(<CompanyPoliciesPage />)} />
          </Route>

          {/* ── HR/Manager routes (/hr/*) ─────────────────────────────── */}
          <Route element={<RoleProtectedRoute allowedRoles={[ROLES.HR, ROLES.MANAGER]} />}>
            <Route path={ROUTES.HR_DASHBOARD} element={withSuspense(<HRDashboardPage />)} />
            <Route path={ROUTES.HR_EMPLOYEES} element={withSuspense(<EmployeesPage />)} />
            <Route
              path={`${ROUTES.HR_EMPLOYEES}/:id`}
              element={withSuspense(<EmployeeDetailsPage />)}
            />
            <Route path={ROUTES.HR_DEPARTMENTS} element={withSuspense(<DepartmentsPage />)} />
            <Route
              path={`${ROUTES.HR_DEPARTMENTS}/:id`}
              element={withSuspense(<DepartmentDetailsPage />)}
            />
            <Route path={ROUTES.HR_LEAVES} element={withSuspense(<LeavesPage />)} />
            <Route path={ROUTES.HR_ATTENDANCE} element={withSuspense(<AttendancePage />)} />
            <Route path={ROUTES.HR_REVIEWS} element={withSuspense(<ReviewsPage />)} />
            <Route path={ROUTES.HR_POLICIES} element={withSuspense(<CompanyPoliciesPage />)} />
            <Route path={ROUTES.MANAGER_LEAVES} element={withSuspense(<ManagerLeavePage />)} />
            <Route path={ROUTES.MANAGER_TASKS} element={withSuspense(<ManagerTasksPage />)} />
            <Route
              path={`${ROUTES.MANAGER_TASKS}/:id`}
              element={withSuspense(<ManagerTaskDetailPage />)}
            />
            <Route
              path={ROUTES.MANAGER_TASK_REVIEWS}
              element={withSuspense(<ManagerTasksPage />)}
            />
          </Route>

          {/* ── Employee self-service routes (/employee/*) ────────────── */}
          <Route element={<RoleProtectedRoute allowedRoles={[ROLES.EMPLOYEE]} />}>
            <Route
              path={ROUTES.EMPLOYEE_DASHBOARD}
              element={withSuspense(<EmployeeDashboardPage />)}
            />
            <Route path={ROUTES.EMPLOYEE_LEAVES} element={withSuspense(<MyLeavesPage />)} />
            <Route path={ROUTES.EMPLOYEE_ATTENDANCE} element={withSuspense(<AttendancePage />)} />
            <Route path={ROUTES.EMPLOYEE_PROFILE} element={withSuspense(<ProfilePage />)} />
            <Route path={ROUTES.EMPLOYEE_REVIEWS} element={withSuspense(<ReviewsPage />)} />
            <Route path={ROUTES.EMPLOYEE_TASKS} element={withSuspense(<EmployeeTasksPage />)} />
            <Route
              path={`${ROUTES.EMPLOYEE_TASKS}/:id`}
              element={withSuspense(<EmployeeTaskDetailPage />)}
            />
          </Route>

          {/* ── Legacy flat routes (backward compatibility) ───────────── */}
          <Route path={ROUTES.EMPLOYEES} element={withSuspense(<EmployeesPage />)} />
          <Route path={`${ROUTES.EMPLOYEES}/:id`} element={withSuspense(<EmployeeDetailsPage />)} />
          <Route path={ROUTES.DEPARTMENTS} element={withSuspense(<DepartmentsPage />)} />
          <Route
            path={`${ROUTES.DEPARTMENTS}/:id`}
            element={withSuspense(<DepartmentDetailsPage />)}
          />
          <Route path={ROUTES.LEAVES} element={withSuspense(<LeavesPage />)} />
          <Route path={ROUTES.MY_LEAVES} element={withSuspense(<MyLeavesPage />)} />
          <Route path={`${ROUTES.LEAVES}/:id`} element={withSuspense(<LeaveDetailsPage />)} />
          <Route path={ROUTES.ATTENDANCE} element={withSuspense(<AttendancePage />)} />
          <Route path={ROUTES.REVIEWS} element={withSuspense(<ReviewsPage />)} />
          <Route path={ROUTES.PROFILE} element={withSuspense(<ProfilePage />)} />
          <Route path={ROUTES.SETTINGS} element={withSuspense(<SettingsPage />)} />
          {/* ── AI Assistant (all authenticated roles) ─────────────────── */}
          <Route path={ROUTES.AI_ASSISTANT} element={withSuspense(<AiAssistantPage />)} />
        </Route>
      </Route>

      {/* ── Error pages ── */}
      <Route path={ROUTES.ACCESS_DENIED} element={withSuspense(<AccessDeniedPage />)} />
      <Route path={ROUTES.NOT_FOUND} element={withSuspense(<NotFoundPage />)} />
      <Route path="*" element={<Navigate to={ROUTES.NOT_FOUND} replace />} />
    </Routes>
  );
}
