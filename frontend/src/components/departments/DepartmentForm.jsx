/**
 * @fileoverview DepartmentForm — React Hook Form + Zod form for create/update.
 *
 * Fields:
 *   - Name (required)
 *   - Code (required, uppercase hint)
 *   - Description (optional, multiline)
 *   - Department Head (optional, free text)
 *
 * Server violation errors (RFC 7807) are mapped onto form fields via setError.
 */

import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Grid, TextField, Typography } from '@mui/material';
import { departmentSchema } from '@/utils/validationSchemas';
import { DEPARTMENT_FORM_DEFAULTS } from '@/constants/departmentConstants';

/**
 * @typedef {Object} DepartmentFormProps
 * @property {string}          formId
 * @property {Partial<import('@/services/departmentApi').DepartmentResponse>} [defaultValues]
 * @property {(data: Object) => void} onSubmit
 * @property {boolean}         [isSubmitting]
 * @property {Record<string, string>} [serverErrors]
 */

/**
 * Create / Edit department form.
 *
 * @param {DepartmentFormProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentForm({
  formId,
  defaultValues,
  onSubmit,
  isSubmitting = false,
  serverErrors,
}) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(departmentSchema),
    defaultValues: { ...DEPARTMENT_FORM_DEFAULTS, ...defaultValues },
  });

  // Map server violations onto RHF fields
  useEffect(() => {
    if (!serverErrors) return;
    Object.entries(serverErrors).forEach(([field, message]) => {
      setError(field, { type: 'server', message });
    });
  }, [serverErrors, setError]);

  return (
    <Box
      component="form"
      id={formId}
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      aria-label="Department form"
    >
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        Department Information
      </Typography>

      <Grid container spacing={2}>
        {/* Name */}
        <Grid size={{ xs: 12, sm: 8 }}>
          <TextField
            {...register('name')}
            label="Department Name"
            fullWidth
            required
            error={Boolean(errors.name)}
            helperText={errors.name?.message}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Department name' }}
          />
        </Grid>

        {/* Code */}
        <Grid size={{ xs: 12, sm: 4 }}>
          <TextField
            {...register('code')}
            label="Code"
            fullWidth
            required
            error={Boolean(errors.code)}
            helperText={errors.code?.message ?? 'Short unique identifier (e.g. ENG)'}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Department code', style: { textTransform: 'uppercase' } }}
          />
        </Grid>

        {/* Description */}
        <Grid size={{ xs: 12 }}>
          <TextField
            {...register('description')}
            label="Description"
            fullWidth
            multiline
            rows={3}
            error={Boolean(errors.description)}
            helperText={errors.description?.message}
            disabled={isSubmitting}
            placeholder="Brief description of this department's function…"
            inputProps={{ 'aria-label': 'Department description' }}
          />
        </Grid>

        {/* Department Head */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('headName')}
            label="Department Head"
            fullWidth
            error={Boolean(errors.headName)}
            helperText={
              errors.headName?.message ?? 'Optional: name of the person leading this department'
            }
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Department head name' }}
          />
        </Grid>
      </Grid>
    </Box>
  );
}
