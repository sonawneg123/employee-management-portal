/**
 * @fileoverview TaskForm — shared form for creating and editing tasks.
 * Used by both manager task creation and task edit dialogs.
 *
 * Phase 6C: category is now a Select with enum values; URGENT replaces CRITICAL;
 *           showAvailability prop enables the EmployeeAvailabilitySelector.
 */

import React from 'react';
import {
  Box,
  Button,
  CircularProgress,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material';

import EmployeeAvailabilitySelector from '@/components/tasks/EmployeeAvailabilitySelector';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

/** Enum values returned by the backend for task category. */
export const TASK_CATEGORIES = [
  'DEVELOPMENT',
  'TESTING',
  'DOCUMENTATION',
  'DEVOPS',
  'HR',
  'SUPPORT',
  'RESEARCH',
  'OTHER',
];

/** Display-friendly category label. */
export function categoryLabel(cat) {
  if (!cat) return '';
  return cat.charAt(0) + cat.slice(1).toLowerCase();
}

/**
 * @param {{
 *   values: Object,
 *   errors: Object,
 *   onChange: (field: string, value: any) => void,
 *   onSubmit: () => void,
 *   onCancel: () => void,
 *   employees: Array,
 *   isSubmitting: boolean,
 *   submitLabel?: string,
 *   showAvailability?: boolean,
 * }} props
 * @returns {JSX.Element}
 */
export default function TaskForm({
  values,
  errors = {},
  onChange,
  onSubmit,
  onCancel,
  employees = [],
  isSubmitting = false,
  submitLabel = 'Create Task',
  showAvailability = false,
}) {
  const handleField = (field) => (e) => onChange(field, e.target.value);

  return (
    <Box
      component="form"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit();
      }}
    >
      <Grid container spacing={2}>
        {/* Title */}
        <Grid size={{ xs: 12 }}>
          <TextField
            label="Title"
            value={values.title ?? ''}
            onChange={handleField('title')}
            error={Boolean(errors.title)}
            helperText={errors.title}
            fullWidth
            required
            inputProps={{ maxLength: 255 }}
          />
        </Grid>

        {/* Description */}
        <Grid size={{ xs: 12 }}>
          <TextField
            label="Description"
            value={values.description ?? ''}
            onChange={handleField('description')}
            error={Boolean(errors.description)}
            helperText={errors.description}
            fullWidth
            multiline
            rows={3}
          />
        </Grid>

        {/* Guidelines */}
        <Grid size={{ xs: 12 }}>
          <TextField
            label="Guidelines"
            value={values.guidelines ?? ''}
            onChange={handleField('guidelines')}
            fullWidth
            multiline
            rows={3}
            placeholder="Step-by-step instructions for the assignee…"
          />
        </Grid>

        {/* Acceptance Criteria */}
        <Grid size={{ xs: 12 }}>
          <TextField
            label="Acceptance Criteria"
            value={values.acceptanceCriteria ?? ''}
            onChange={handleField('acceptanceCriteria')}
            fullWidth
            multiline
            rows={3}
            placeholder="What must be true for this task to be considered done…"
          />
        </Grid>

        {/* Assign to employee */}
        <Grid size={{ xs: 12, sm: 6 }}>
          {showAvailability ? (
            <EmployeeAvailabilitySelector
              employees={employees}
              value={values.assignedEmployeeId ?? ''}
              onChange={(val) => onChange('assignedEmployeeId', val)}
              error={errors.assignedEmployeeId}
            />
          ) : (
            <FormControl fullWidth>
              <InputLabel id="task-employee-label">Assign To</InputLabel>
              <Select
                labelId="task-employee-label"
                label="Assign To"
                value={values.assignedEmployeeId ?? ''}
                onChange={handleField('assignedEmployeeId')}
              >
                <MenuItem value="">
                  <em>Unassigned (Draft)</em>
                </MenuItem>
                {employees.map((emp) => (
                  <MenuItem key={emp.id} value={emp.id}>
                    {emp.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
        </Grid>

        {/* Priority */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth>
            <InputLabel id="task-priority-label">Priority</InputLabel>
            <Select
              labelId="task-priority-label"
              label="Priority"
              value={values.priority ?? 'MEDIUM'}
              onChange={handleField('priority')}
            >
              {PRIORITIES.map((p) => (
                <MenuItem key={p} value={p}>
                  {p.charAt(0) + p.slice(1).toLowerCase()}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>

        {/* Due Date */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            label="Due Date"
            type="date"
            value={values.dueDate ?? ''}
            onChange={handleField('dueDate')}
            error={Boolean(errors.dueDate)}
            helperText={errors.dueDate}
            fullWidth
            required
            InputLabelProps={{ shrink: true }}
          />
        </Grid>

        {/* Estimated Hours */}
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            label="Estimated Hours"
            type="number"
            value={values.estimatedHours ?? ''}
            onChange={handleField('estimatedHours')}
            fullWidth
            inputProps={{ min: 0, step: 0.5 }}
          />
        </Grid>

        {/* Category — enum Select */}
        <Grid size={{ xs: 12 }}>
          <FormControl fullWidth>
            <InputLabel id="task-category-label">Category</InputLabel>
            <Select
              labelId="task-category-label"
              label="Category"
              value={values.category ?? ''}
              onChange={handleField('category')}
            >
              <MenuItem value="">
                <em>None</em>
              </MenuItem>
              {TASK_CATEGORIES.map((cat) => (
                <MenuItem key={cat} value={cat}>
                  {categoryLabel(cat)}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>
      </Grid>

      {/* Actions */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 3 }}>
        <Button variant="outlined" onClick={onCancel} disabled={isSubmitting}>
          Cancel
        </Button>
        <Button
          type="submit"
          variant="contained"
          disabled={isSubmitting}
          startIcon={isSubmitting ? <CircularProgress size={16} /> : null}
        >
          {submitLabel}
        </Button>
      </Box>
    </Box>
  );
}
