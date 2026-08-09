/**
 * @fileoverview Shared Zod validation schemas for form components.
 *
 * Centralising schemas here keeps form components free of inline schema
 * definitions and makes it easy to keep client-side validation in sync
 * with the backend's Bean Validation constraints.
 *
 * Schemas exported:
 *   {@link employeeSchema}   — CreateEmployeeRequest / UpdateEmployeeRequest
 *   {@link departmentSchema} — CreateDepartmentRequest / UpdateDepartmentRequest
 */

import { z } from 'zod';

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

  email: z
    .string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address'),

  employeeCode: z
    .string()
    .min(1, 'Employee code is required')
    .max(20, 'Employee code must not exceed 20 characters'),

  jobTitle: z
    .string()
    .min(1, 'Job title is required')
    .max(100, 'Job title must not exceed 100 characters'),

  departmentId: z
    .string()
    .min(1, 'Department is required'),

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

  dateOfJoining: z
    .string()
    .min(1, 'Hire date is required'),

  salary: z
    .union([z.number(), z.string()])
    .transform((val) => (val === '' ? NaN : Number(val)))
    .refine((val) => !isNaN(val), { message: 'Salary is required' })
    .refine((val) => val >= 0, { message: 'Salary must be a positive number' }),

  status: z
    .enum(
      ['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED'],
      { errorMap: () => ({ message: 'Please select a valid status' }) },
    ),

  managerId: z.string().optional().or(z.literal('')),

  profilePhotoUrl: z
    .string()
    .url('Profile photo URL must be a valid URL')
    .optional()
    .or(z.literal('')),
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
