/**
 * @fileoverview LoginPage — premium split-screen sign-in page.
 *
 * Accepts an optional {@code targetRole} prop so that role-specific login
 * routes (/login/admin, /login/hr, /login/employee) can display appropriate
 * branding. Composes AuthLayout, AuthCard, and LoginForm.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { Helmet } from 'react-helmet-async';
import AuthLayout from '@/components/auth/AuthLayout';
import AuthCard from '@/components/auth/AuthCard';
import LoginForm from '@/components/auth/LoginForm';

/** Maps a targetRole to human-readable label for AuthCard / Helmet. */
const ROLE_LABELS = {
  ROLE_ADMIN: 'Admin',
  ROLE_HR: 'HR',
  ROLE_EMPLOYEE: 'Employee',
};

/**
 * Login page.
 *
 * @param {{ targetRole?: string }} props
 * @returns {JSX.Element}
 */
export default function LoginPage({ targetRole }) {
  const roleLabel = ROLE_LABELS[targetRole] ?? null;
  const pageTitle = roleLabel ? `Sign In as ${roleLabel}` : 'Sign In';
  const cardDescription = roleLabel
    ? `Enter your ${roleLabel} credentials to access your account.`
    : 'Enter your credentials to access your account.';

  return (
    <>
      <Helmet>
        <title>{pageTitle} — PeopleCore HR</title>
        <meta name="description" content="Sign in to PeopleCore HR to manage your workforce." />
      </Helmet>

      <AuthLayout
        title="Welcome back 👋"
        subtitle="Sign in to manage your workforce, track attendance, and review performance all in one place."
      >
        <AuthCard title={pageTitle} description={cardDescription}>
          <LoginForm />
        </AuthCard>
      </AuthLayout>
    </>
  );
}

LoginPage.propTypes = {
  targetRole: PropTypes.string,
};
