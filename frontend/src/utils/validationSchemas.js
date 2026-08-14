/**
 * @fileoverview Shared Zod validation schemas for form components.
 *
 * Centralising schemas here keeps form components free of inline schema
 * definitions and makes it easy to keep client-side validation in sync
 * with the backend's Bean Validation constraints.
 *
 * Schemas exported:
 *   {@link loginSchema}      — LoginForm
 *   {@link registerSchema}   — RegisterForm
 *   {@link employeeSchema}   — CreateEmployeeRequest / UpdateEmployeeRequest
 *   {@link departmentSchema} — CreateDepartmentRequest / UpdateDepartmentRequest
 */

import { z } from 'zod';

// ── Login schema ──────────────────────────────────────────────────────────────

/**
 * Zod schema for the login form.
 *
 * @type {z.ZodObject}
 */
export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Please enter a valid email address'),

  password: z.string().min(1, 'Password is required'),

  rememberMe: z.boolean().optional(),
});

// ── Register schema ───────────────────────────────────────────────────────────

/**
 * Zod schema for the registration form.
 *
 * @type {z.ZodObject}
 */
export const registerSchema = z
  .object({
    firstName: z
      .string()
      .min(1, 'First name is required')
      .max(100, 'First name must not exceed 100 characters'),

    lastName: z
      .string()
      .min(1, 'Last name is required')
      .max(100, 'Last name must not exceed 100 characters'),

    email: z.string().min(1, 'Email is required').email('Please enter a valid email address'),

    password: z.string().min(8, 'Password must be at least 8 characters'),

    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

// ── Employee schema ───────────────────────────────────────────────────────────

/**
 * Zod schema for the create / update employee form.
 *
 * Mirrors the backend {@code CreateEmployeeRequest} Bean Validation constraints:
 *   - firstName, lastName: @NotBlank
 *   - email: @NotBlank + @Email
 *   - employeeCode: @NotBlank
 *   - jobTitle: @NotBlank
 *   - departmentId: @NotBlank (UUID)
 *   - dateOfJoining: @NotNull (date string)
 *   - salary: @NotNull + @PositiveOrZero
 *   - status: @NotNull (enum)
 *
 * @type {z.ZodObject}
 */
export const employeeSchema = z.object({
  firstName: z
    .string()
    .min(1, 'First name is required')
    .max(100, 'First name must not exceed 100 characters'),

  lastName: z
    .string()
    .min(1, 'Last name is required')
    .max(100, 'Last name must not exceed 100 characters'),

  email: z.string().min(1, 'Email is required').email('Please enter a valid email address'),

  employeeCode: z
    .string()
    .min(1, 'Employee code is required')
    .max(20, 'Employee code must not exceed 20 characters'),

  jobTitle: z
    .string()
    .min(1, 'Job title is required')
    .max(100, 'Job title must not exceed 100 characters'),

  departmentId: z.string().min(1, 'Department is required'),

  phone: z
    .string()
    .max(30, 'Phone number must not exceed 30 characters')
    .optional()
    .or(z.literal('')),

  address: z
    .string()
    .max(255, 'Address must not exceed 255 characters')
    .optional()
    .or(z.literal('')),

  dateOfJoining: z.string().min(1, 'Hire date is required'),

  salary: z
    .union([z.number(), z.string()])
    .transform((val) => (val === '' ? NaN : Number(val)))
    .refine((val) => !isNaN(val), { message: 'Salary is required' })
    .refine((val) => val >= 0, { message: 'Salary must be a positive number' }),

  status: z.enum(['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED'], {
    errorMap: () => ({ message: 'Please select a valid status' }),
  }),

  managerId: z.string().optional().or(z.literal('')),

  profilePhotoUrl: z
    .string()
    .url('Profile photo URL must be a valid URL')
    .optional()
    .or(z.literal('')),
});

// ── Forgot password / OTP / reset-password schemas ────────────────────────────

/**
 * Zod schema for the "enter email" step of the forgot-password flow.
 *
 * @type {z.ZodObject}
 */
export const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Please enter a valid email address'),
});

/**
 * Zod schema for the "enter OTP" step.
 *
 * @type {z.ZodObject}
 */
export const verifyOtpSchema = z.object({
  otp: z
    .string()
    .min(1, 'OTP is required')
    .length(6, 'OTP must be exactly 6 digits')
    .regex(/^\d{6}$/, 'OTP must contain only digits'),
});

/**
 * Zod schema for the "set new password" step.
 *
 * @type {z.ZodObject}
 */
export const resetPasswordSchema = z
  .object({
    newPassword: z.string().min(8, 'Password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

// ── Department schema ─────────────────────────────────────────────────────────

/**
 * Zod schema for the create / update department form.
 *
 * Mirrors the backend {@code CreateDepartmentRequest} Bean Validation constraints:
 *   - name: @NotBlank
 *   - code: @NotBlank
 *   - description, headName: optional
 *
 * @type {z.ZodObject}
 */
export const departmentSchema = z.object({
  name: z
    .string()
    .min(1, 'Department name is required')
    .max(100, 'Department name must not exceed 100 characters'),

  code: z
    .string()
    .min(1, 'Department code is required')
    .max(20, 'Department code must not exceed 20 characters'),

  description: z
    .string()
    .max(500, 'Description must not exceed 500 characters')
    .optional()
    .or(z.literal('')),

  headName: z
    .string()
    .max(100, 'Head name must not exceed 100 characters')
    .optional()
    .or(z.literal('')),
});
