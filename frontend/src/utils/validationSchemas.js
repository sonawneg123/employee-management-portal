/**
 * @fileoverview Shared Zod validation schemas and helper functions.
 *
 * Centralising schema definitions ensures that the same rules are applied
 * consistently across all React Hook Form forms. Import individual schemas
 * directly into form components.
 */

import { z } from 'zod';

// ─── Reusable field schemas ────────────────────────────────────────────────

/**
 * Validates an email address.
 *
 * @type {import('zod').ZodString}
 */
export const emailSchema = z
  .string()
  .min(1, 'Email is required')
  .email('Please enter a valid email address')
  .max(150, 'Email must not exceed 150 characters');

/**
 * Validates a password (minimum 8 characters).
 *
 * @type {import('zod').ZodString}
 */
export const passwordSchema = z
  .string()
  .min(8, 'Password must be at least 8 characters')
  .max(100, 'Password must not exceed 100 characters');

/**
 * Validates a non-blank string with configurable max length.
 *
 * @param {string} fieldName  - Human-readable name shown in errors.
 * @param {number} [max=100]  - Maximum allowed length.
 * @returns {import('zod').ZodString}
 */
export function requiredString(fieldName, max = 100) {
  return z
    .string()
    .min(1, `${fieldName} is required`)
    .max(max, `${fieldName} must not exceed ${max} characters`);
}

/**
 * Validates a UUID v4 string.
 *
 * @type {import('zod').ZodString}
 */
export const uuidSchema = z.string().uuid('Must be a valid UUID');

/**
 * Validates a positive decimal number (salary, etc.).
 *
 * @type {import('zod').ZodNumber}
 */
export const positiveDecimalSchema = z
  .number({ invalid_type_error: 'Must be a number' })
  .min(0, 'Value must be zero or positive');

// ─── Full form schemas ─────────────────────────────────────────────────────

/**
 * Zod schema for the login form.
 *
 * @type {import('zod').ZodObject<any>}
 */
export const loginSchema = z.object({
  email:      emailSchema,
  password:   z.string().min(1, 'Password is required'),
  rememberMe: z.boolean().optional().default(false),
});

/**
 * Zod schema for the registration form.
 *
 * @type {import('zod').ZodObject<any>}
 */
export const registerSchema = z
  .object({
    email:           emailSchema,
    password:        passwordSchema,
    confirmPassword: z.string().min(1, 'Please confirm your password'),
    firstName:       requiredString('First name'),
    lastName:        requiredString('Last name'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path:    ['confirmPassword'],
  });

/**
 * Zod schema for the create/update employee form.
 * Covers all required and optional fields sent to the backend.
 *
 * @type {import('zod').ZodObject<any>}
 */
export const employeeSchema = z.object({
  // Identity
  employeeCode:    requiredString('Employee code', 20),
  firstName:       requiredString('First name', 50),
  lastName:        requiredString('Last name', 50),
  email:           emailSchema,
  // Role
  jobTitle:        requiredString('Job title', 150),
  departmentId:    uuidSchema,
  // Contact
  phone:           z.string().max(20, 'Phone must not exceed 20 characters').optional().or(z.literal('')),
  address:         z.string().max(255, 'Address must not exceed 255 characters').optional().or(z.literal('')),
  // Employment
  dateOfJoining:   z.string().min(1, 'Date of joining is required'),
  salary:          positiveDecimalSchema,
  status:          z.enum(['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED'], {
    errorMap: () => ({ message: 'Please select a valid status' }),
  }),
  // Optional
  managerId:       z.string().uuid('Must be a valid UUID').optional().or(z.literal('')),
  profilePhotoUrl: z.string().url('Must be a valid URL').optional().or(z.literal('')),
});

/**
 * Zod schema for the create/update department form.
 *
 * @type {import('zod').ZodObject<any>}
 */
export const departmentSchema = z.object({
  name:        requiredString('Department name', 100),
  code:        requiredString('Department code', 20),
  description: z.string().max(500, 'Description must not exceed 500 characters').optional().or(z.literal('')),
  headName:    z.string().max(100, 'Head name must not exceed 100 characters').optional().or(z.literal('')),
});
