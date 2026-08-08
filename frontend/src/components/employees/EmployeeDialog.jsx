/**
 * @fileoverview EmployeeDialog — modal dialog wrapping EmployeeForm.
 *
 * Handles both create (no defaultValues) and edit (with defaultValues) modes.
 * The submit button is rendered in the DialogActions and connected to the
 * form via its `id` prop so the form can live inside DialogContent without
 * a nested button.
 */

import React from 'react';
import {
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Tooltip,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import EmployeeForm from './EmployeeForm';

const FORM_ID = 'employee-dialog-form';

/**
 * @typedef {Object} EmployeeDialogProps
 * @property {boolean}          open
 * @property {'create'|'edit'}  mode
 * @property {Partial<import('@/services/employeeApi').EmployeeResponse>} [defaultValues]
 * @property {boolean}          isSubmitting
 * @property {Record<string,string>} [serverErrors]
 * @property {(data: Object) => void} onSubmit
 * @property {() => void}       onClose
 */

/**
 * Responsive dialog for creating or editing an employee record.
 *
 * @param {EmployeeDialogProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeDialog({
  open,
  mode,
  defaultValues,
  isSubmitting,
  serverErrors,
  onSubmit,
  onClose,
}) {
  const isEdit = mode === 'edit';
  const title  = isEdit ? 'Edit Employee' : 'Add New Employee';

  return (
    <Dialog
      open={open}
      onClose={isSubmitting ? undefined : onClose}
      maxWidth="md"
      fullWidth
      scroll="paper"
      aria-labelledby="employee-dialog-title"
    >
      <DialogTitle
        id="employee-dialog-title"
        sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
      >
        {title}
        <Tooltip title="Close">
          <IconButton
            onClick={onClose}
            disabled={isSubmitting}
            size="small"
            aria-label="Close dialog"
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </DialogTitle>

      <DialogContent dividers>
        <EmployeeForm
          formId={FORM_ID}
          defaultValues={defaultValues}
          onSubmit={onSubmit}
          isSubmitting={isSubmitting}
          serverErrors={serverErrors}
        />
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button
          onClick={onClose}
          disabled={isSubmitting}
          aria-label="Cancel"
        >
          Cancel
        </Button>
        <Button
          type="submit"
          form={FORM_ID}
          variant="contained"
          disabled={isSubmitting}
          startIcon={isSubmitting ? <CircularProgress size={16} color="inherit" /> : null}
          aria-label={isEdit ? 'Save changes' : 'Create employee'}
        >
          {isSubmitting ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Employee'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
