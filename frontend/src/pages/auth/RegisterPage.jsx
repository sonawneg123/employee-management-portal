/**
 * @fileoverview RegisterPage — premium account creation page.
 *
 * Accepts an optional {@code defaultRole} prop so that role-specific
 * registration routes (/register/hr, /register/employee) can display
 * appropriate branding and pass the role to RegisterForm.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { Helmet } from 'react-helmet-async';
import AuthLayout from '@/components/auth/AuthLayout';
import AuthCard from '@/components/auth/AuthCard';
import RegisterForm from '@/components/auth/RegisterForm';

/** Maps a defaultRole to human-readable copy. */
const ROLE_COPY = {
  ROLE_HR: {
    title: 'Create HR Account',
    description: 'Fill in your details to create an HR account.',
  },
  ROLE_EMPLOYEE: {
    title: 'Create Employee Account',
    description: 'Fill in your details to create an Employee account.',
  },
};

/**
 * Register page.
 *
 * @param {{ defaultRole?: string }} props
 * @returns {JSX.Element}
 */
export default function RegisterPage({ defaultRole }) {
  const copy = ROLE_COPY[defaultRole] ?? {
    title: 'Create Account',
    description: 'Fill in your details to get started.',
  };

  return (
    <>
      <Helmet>
        <title>{copy.title} — PeopleCore HR</title>
        <meta name="description" content="Create your PeopleCore HR account." />
      </Helmet>

      <AuthLayout
        title="Join your team 🚀"
        subtitle="Create an account to access your employee dashboard, leave requests, and performance reviews."
      >
        <AuthCard title={copy.title} description={copy.description}>
          <RegisterForm role={defaultRole} />
        </AuthCard>
      </AuthLayout>
    </>
  );
}

RegisterPage.propTypes = {
  defaultRole: PropTypes.string,
};
