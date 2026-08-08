/**
 * @fileoverview RegisterPage — full implementation.
 *
 * Composes {@link AuthLayout}, {@link AuthCard}, and {@link RegisterForm}
 * into the complete registration page.
 */

import React from 'react';
import { Helmet } from 'react-helmet-async';
import AuthLayout    from '@/components/auth/AuthLayout';
import AuthCard      from '@/components/auth/AuthCard';
import RegisterForm  from '@/components/auth/RegisterForm';

/**
 * Register page — for new user account creation.
 *
 * @returns {JSX.Element}
 */
export default function RegisterPage() {
  return (
    <>
      {typeof Helmet !== 'undefined' && (
        <Helmet>
          <title>Create Account — Employee Management Portal</title>
          <meta name="description" content="Create a new Employee Management Portal account" />
        </Helmet>
      )}

      <AuthLayout
        title="Join EMP Portal today"
        subtitle="Create your account to start managing employees, tracking leaves, and reviewing performance."
      >
        <AuthCard
          title="Create Account"
          description="Fill in your details below to get started."
        >
          <RegisterForm />
        </AuthCard>
      </AuthLayout>
    </>
  );
}
