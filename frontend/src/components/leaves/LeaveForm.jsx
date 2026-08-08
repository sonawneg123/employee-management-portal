/**
 * @fileoverview LeaveForm — React Hook Form + Zod form for leave requests.
 *
 * Fields:
 *   - Leave Type (required select)
 *   - Start Date (required date picker)
 *   - End Date   (required date picker, must be ≥ start)
 *   - Reason     (optional textarea)
 *   - Emergency Flag (optional checkbox)
 *   - Attachment URL (optional)
 *
 * Auto-calculates and displays working days on date change.
 */

import React, { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Box,
  Checkbox,
  Chip,
  FormControl,
  FormControlLabel,
  FormHelperText,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import {
  LEAVE_TYPE_OPTIONS,
  LEAVE_FORM_DEFAULTS,
} from '@/constants/leaveConstants';
import { countWorkingDays, validateDateRange } from '@/utils/leaveCalculations';

// ── Zod schema ────────────────────────────────────────────────────────────────

const leaveFormSchema = z
  .object({
    leaveType:     z.enum(
      ['ANNUAL','SICK','MATERNITY','PATERNITY','UNPAID','EMERGENCY','STUDY','OTHER'],
      { errorMap: () => ({ message: 'Please select a leave type' }) },
    ),
    startDate:     z.string().min(1, 'Start date is required'),
    endDate:       z.string().min(1, 'End date is required'),
    reason:        z.string().max(500, 'Reason must not exceed 500 characters').optional().or(z.literal('')),
    isEmergency:   z.boolean().optional(),
    attachmentUrl: z.string().url('Must be a valid URL').optional().or(z.literal('')),
  })
  .refine(
    (data) => {
      if (!data.startDate || !data.endDate) return true;
      return validateDateRange(data.startDate, data.endDate).valid;
    },
    (data) => ({
      message: validateDateRange(data.startDate, data.endDate).message,
      path:    ['endDate'],
    }),
  );

// ── Component ─────────────────────────────────────────────────────────────────

/**
 * @typedef {Object} LeaveFormProps
 * @property {string}          formId
 * @property {Object}          [defaultValues]
 * @property {(data: Object) => void} onSubmit
 * @property {boolean}         [isSubmitting]
 * @property {Record<string, string>} [serverErrors]
 */

/**
 * Leave request create / edit form.
 *
 * @param {LeaveFormProps} props
 * @returns {JSX.Element}
 */
export default function LeaveForm({ formId, defaultValues, onSubmit, isSubmitting = false, serverErrors }) {
  const {
    register,
    handleSubmit,
    control,
    watch,
    setError,
    formState: { errors },
  } = useForm({
    resolver:      zodResolver(leaveFormSchema),
    defaultValues: { ...LEAVE_FORM_DEFAULTS, ...defaultValues },
  });

  // Map server violations
  useEffect(() => {
    if (!serverErrors) return;
    Object.entries(serverErrors).forEach(([f, m]) => setError(f, { type: 'server', message: m }));
  }, [serverErrors, setError]);

  const startDate = watch('startDate');
  const endDate   = watch('endDate');
  const workingDays = (startDate && endDate)
    ? countWorkingDays(startDate, endDate)
    : null;

  return (
    <Box
      component="form"
      id={formId}
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      aria-label="Leave request form"
    >
      <Grid container spacing={2}>
        {/* Leave Type */}
        <Grid size={{ xs: 12 }}>
          <Controller
            name="leaveType"
            control={control}
            render={({ field }) => (
              <FormControl fullWidth required error={Boolean(errors.leaveType)} disabled={isSubmitting}>
                <InputLabel id="leave-type-label">Leave Type</InputLabel>
                <Select
                  {...field}
                  labelId="leave-type-label"
                  label="Leave Type"
                  aria-label="Leave type"
                >
                  {LEAVE_TYPE_OPTIONS.map((opt) => (
                    <MenuItem key={opt.value} value={opt.value}>
                      {opt.icon} {opt.label}
                    </MenuItem>
                  ))}
                </Select>
                {errors.leaveType && <FormHelperText>{errors.leaveType.message}</FormHelperText>}
              </FormControl>
            )}
          />
        </Grid>

        {/* Start Date */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('startDate')}
            label="Start Date"
            type="date"
            fullWidth
            required
            error={Boolean(errors.startDate)}
            helperText={errors.startDate?.message}
            disabled={isSubmitting}
            slotProps={{ inputLabel: { shrink: true } }}
            inputProps={{ 'aria-label': 'Start date' }}
          />
        </Grid>

        {/* End Date */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            {...register('endDate')}
            label="End Date"
            type="date"
            fullWidth
            required
            error={Boolean(errors.endDate)}
            helperText={errors.endDate?.message}
            disabled={isSubmitting}
            slotProps={{ inputLabel: { shrink: true } }}
            inputProps={{ 'aria-label': 'End date', min: startDate || undefined }}
          />
        </Grid>

        {/* Working days preview */}
        {workingDays !== null && (
          <Grid size={{ xs: 12 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CalendarTodayIcon fontSize="small" color="primary" />
              <Typography variant="body2" color="text.secondary">
                Duration:
              </Typography>
              <Chip
                label={`${workingDays} working ${workingDays === 1 ? 'day' : 'days'}`}
                color={workingDays === 0 ? 'error' : 'primary'}
                size="small"
                aria-live="polite"
                aria-label={`${workingDays} working days`}
              />
            </Box>
          </Grid>
        )}

        {/* Reason */}
        <Grid size={{ xs: 12 }}>
          <TextField
            {...register('reason')}
            label="Reason"
            fullWidth
            multiline
            rows={3}
            error={Boolean(errors.reason)}
            helperText={errors.reason?.message}
            disabled={isSubmitting}
            placeholder="Briefly describe the reason for your leave request…"
            inputProps={{ 'aria-label': 'Reason' }}
          />
        </Grid>

        {/* Emergency flag */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <Controller
            name="isEmergency"
            control={control}
            render={({ field }) => (
              <FormControlLabel
                control={
                  <Checkbox
                    checked={field.value ?? false}
                    onChange={(e) => field.onChange(e.target.checked)}
                    disabled={isSubmitting}
                    aria-label="Mark as emergency leave"
                  />
                }
                label="Emergency leave"
              />
            )}
          />
        </Grid>

        {/* Attachment URL */}
        <Grid size={{ xs: 12 }}>
          <TextField
            {...register('attachmentUrl')}
            label="Attachment URL"
            fullWidth
            error={Boolean(errors.attachmentUrl)}
            helperText={errors.attachmentUrl?.message ?? 'Optional supporting document link'}
            disabled={isSubmitting}
            placeholder="https://…"
            inputProps={{ 'aria-label': 'Attachment URL' }}
          />
        </Grid>
      </Grid>
    </Box>
  );
}
