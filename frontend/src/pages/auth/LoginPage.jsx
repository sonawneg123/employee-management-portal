/**
 * @fileoverview LoginPage — full implementation.
 *
 * Composes {@link AuthLayout}, {@link AuthCard}, and {@link LoginForm}
 * into the complete login page. Handles:
 * - Session restoration: if the user is already authenticated they are
 *   redirected to the dashboard before this page renders (via PublicRoute).
 * - Redirect param: honours ?redirect=<path> after successful login.
 * - Toast notification on successful login (shown after dashboard render).
 */

import React from 'react';
import { Helmet } from 'react-helmet-async';
import AuthLayout  from '@/components/auth/AuthLayout';
import AuthCard    from '@/components/auth/AuthCard';
import LoginForm   from '@/components/auth/LoginForm';

/**
 * Login page — the entry point for returning users.
 *
 * @returns {JSX.Element}
 */
export default function LoginPage() {
  return (
    <>
      {/* Document title (requires react-helmet-async, gracefully degrades if absent) */}
      {typeof Helmet !== 'undefined' && (
        <Helmet>
          <title>Sign In — Employee Management Portal</title>
          <meta name="description" content="Sign in to the Employee Management Portal" />
        </Helmet>
      )}

      <AuthLayout
        title="Welcome back to EMP Portal"
        subtitle="Sign in to manage your workforce, track attendance, and review performance all in one place."
      >
        <AuthCard
          title="Sign In"
          description="Enter your email and password to access your account."
        >
          <LoginForm />
        </AuthCard>
      </AuthLayout>
    </>
  );
}
