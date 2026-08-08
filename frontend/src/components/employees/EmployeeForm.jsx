/**
 * @fileoverview EmployeeForm — React Hook Form + Zod form for create/update.
 *
 * Renders all fields required by CreateEmployeeRequest / UpdateEmployeeRequest:
 * - Identity: Employee Code, First Name, Last Name, Email
 * - Role:     Job Title, Department (Autocomplete), Manager (optional)
 * - Contact:  Phone, Address
 * - Employment: Hire Date, Salary, Status
 * - Optional: Profile Photo URL
 *
 * Field-level server validation errors (RFC 7807 violations) are mapped
 * onto the form via `setError` so they appear inline below each field.
 */

import React, { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Autocomplete,
  Box,
  Divider,
  FormControl,
  FormHelperText,
  Grid,
  InputAdornment,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import { employeeSchema }            from '@/utils/validationSchemas';
import { useDepartments }            from '@/hooks/useDepartments';
import {
  EMPLOYEE_STATUS_OPTIONS,
  EMPLOYEE_FORM_DEFAULTS,
} from '@/constants/employeeConstants';

/**
 * @typedef {Object} EmployeeFormProps
 * @property {string}          formId           - HTML form id; the dialog submit button uses this.
 * @property {Partial<import('@/services/employeeApi').EmployeeResponse>} [defaultValues]
 *   - Pre-fills the form for edit mode.
 * @property {(data: Object) => void} onSubmit   - Called with validated form data.
 * @property {boolean}         [isSubmitting]   - Disables all fields while the API call is pending.
 * @property {Record<string, string>} [serverErrors] - Field-level errors from the API (violations).
 */

/**
 * Create / Edit employee form.
 *
 * @param {EmployeeFormProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeForm({
  formId,
  defaultValues,
  onSubmit,
  isSubmitting = false,
  serverErrors,
}) {
  const { data: departments, isLoading: deptsLoading } = useDepartments();

  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors },
  } = useForm({
    resolver:      zodResolver(employeeSchema),
    defaultValues: { ...EMPLOYEE_FORM_DEFAULTS, ...defaultValues },
  });

  // Map server-side field violations onto RHF fields
  useEffect(() => {
    if (!serverErrors) return;
    Object.entries(serverErrors).forEach(([field, message]) => {
      setError(field, { type: 'server', message });
    });
  }, [serverErrors, setError]);

  /**
   * Transforms the raw form values before calling the parent onSubmit.
   * Converts salary string → number; removes empty optional strings.
   *
   * @param {Object} values
   */
  const handleFormSubmit = (values) => {
    const payload = {
      ...values,
      salary:          Number(values.salary),
      phone:           values.phone           || undefined,
      address:         values.address         || undefined,
      managerId:       values.managerId       || undefined,
      profilePhotoUrl: values.profilePhotoUrl || undefined,
    };
    onSubmit(payload);
  };

  const selectedDept = departments?.find((d) => d.id === control._formValues?.departmentId) ?? null;

  return (
    <Box
      component="form"
      id={formId}
      onSubmit={handleSubmit(handleFormSubmit)}
      noValidate
      aria-label="Employee form"
    >
      {/* ── Section: Identity ───────────────────────────────────────────── */}
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        Identity
      </Typography>
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('firstName')}
            label="First Name"
            fullWidth
            required
            error={Boolean(errors.firstName)}
            helperText={errors.firstName?.message}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'First name' }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('lastName')}
            label="Last Name"
            fullWidth
            required
            error={Boolean(errors.lastName)}
            helperText={errors.lastName?.message}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Last name' }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('email')}
            label="Email"
            type="email"
            fullWidth
            required
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Email address' }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('employeeCode')}
            label="Employee Code"
            fullWidth
            required
            error={Boolean(errors.employeeCode)}
            helperText={errors.employeeCode?.message}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Employee code' }}
          />
        </Grid>
      </Grid>

      <Divider sx={{ mb: 2 }} />

      {/* ── Section: Role ───────────────────────────────────────────────── */}
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        Role
      </Typography>
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('jobTitle')}
            label="Job Title"
            fullWidth
            required
            error={Boolean(errors.jobTitle)}
            helperText={errors.jobTitle?.message}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Job title' }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <Controller
            name="departmentId"
            control={control}
            render={({ field }) => (
              <Autocomplete
                options={departments ?? []}
                loading={deptsLoading}
                getOptionLabel={(opt) => (typeof opt === 'string' ? opt : opt.name)}
                isOptionEqualToValue={(opt, val) =>
                  opt.id === (typeof val === 'string' ? val : val?.id)
                }
                value={departments?.find((d) => d.id === field.value) ?? null}
                onChange={(_e, newVal) => field.onChange(newVal?.id ?? '')}
                disabled={isSubmitting}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Department"
                    required
                    error={Boolean(errors.departmentId)}
                    helperText={errors.departmentId?.message}
                    inputProps={{
                      ...params.inputProps,
                      'aria-label': 'Department',
                    }}
                  />
                )}
              />
            )}
          />
        </Grid>
      </Grid>

      <Divider sx={{ mb: 2 }} />

      {/* ── Section: Contact ────────────────────────────────────────────── */}
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        Contact
      </Typography>
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('phone')}
            label="Phone"
            fullWidth
            error={Boolean(errors.phone)}
            helperText={errors.phone?.message}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Phone number' }}
          />
        </Grid>
        <Grid size={{ xs: 12 }}>
          <TextField
            {...register('address')}
            label="Address"
            fullWidth
            multiline
            rows={2}
            error={Boolean(errors.address)}
            helperText={errors.address?.message}
            disabled={isSubmitting}
            inputProps={{ 'aria-label': 'Address' }}
          />
        </Grid>
      </Grid>

      <Divider sx={{ mb: 2 }} />

      {/* ── Section: Employment ─────────────────────────────────────────── */}
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        Employment
      </Typography>
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('dateOfJoining')}
            label="Hire Date"
            type="date"
            fullWidth
            required
            error={Boolean(errors.dateOfJoining)}
            helperText={errors.dateOfJoining?.message}
            disabled={isSubmitting}
            slotProps={{ inputLabel: { shrink: true } }}
            inputProps={{ 'aria-label': 'Hire date' }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('salary', { valueAsNumber: true })}
            label="Salary"
            type="number"
            fullWidth
            required
            error={Boolean(errors.salary)}
            helperText={errors.salary?.message}
            disabled={isSubmitting}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">$</InputAdornment>
                ),
              },
            }}
            inputProps={{ 'aria-label': 'Salary', min: 0, step: '0.01' }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <Controller
            name="status"
            control={control}
            render={({ field }) => (
              <FormControl fullWidth required error={Boolean(errors.status)} disabled={isSubmitting}>
                <InputLabel id="status-label">Status</InputLabel>
                <Select
                  {...field}
                  labelId="status-label"
                  label="Status"
                  aria-label="Employee status"
                >
                  {EMPLOYEE_STATUS_OPTIONS.map((opt) => (
                    <MenuItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </MenuItem>
                  ))}
                </Select>
                {errors.status && (
                  <FormHelperText>{errors.status.message}</FormHelperText>
                )}
              </FormControl>
            )}
          />
        </Grid>
      </Grid>

      <Divider sx={{ mb: 2 }} />

      {/* ── Section: Optional ───────────────────────────────────────────── */}
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        Optional
      </Typography>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('profilePhotoUrl')}
            label="Profile Photo URL"
            fullWidth
            error={Boolean(errors.profilePhotoUrl)}
            helperText={errors.profilePhotoUrl?.message}
            disabled={isSubmitting}
            placeholder="https://…"
            inputProps={{ 'aria-label': 'Profile photo URL' }}
          />
        </Grid>
      </Grid>
    </Box>
  );
}
