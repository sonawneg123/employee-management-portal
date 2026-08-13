/**
 * @fileoverview DepartmentDialog — modal dialog wrapping DepartmentForm.
 *
 * Handles both create and edit modes. The submit button is placed in
 * DialogActions and connected to the form via its HTML `id`.
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
import DepartmentForm from './DepartmentForm';

const FORM_ID = 'department-dialog-form';

/**
 * @typedef {Object} DepartmentDialogProps
 * @property {boolean}          open
 * @property {'create'|'edit'}  mode
 * @property {Partial<import('@/services/departmentApi').DepartmentResponse>} [defaultValues]
 * @property {boolean}          isSubmitting
 * @property {Record<string,string>} [serverErrors]
 * @property {(data: Object) => void} onSubmit
 * @property {() => void}       onClose
 */

/**
 * Responsive dialog for creating or editing a department.
 *
 * @param {DepartmentDialogProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentDialog({
  open,
  mode,
  defaultValues,
  isSubmitting,
  serverErrors,
  onSubmit,
  onClose,
}) {
  const isEdit = mode === 'edit';
  const title = isEdit ? 'Edit Department' : 'Add New Department';

  return (
    <Dialog
      open={open}
      onClose={isSubmitting ? undefined : onClose}
      maxWidth="sm"
      fullWidth
      scroll="paper"
      aria-labelledby="dept-dialog-title"
    >
      <DialogTitle
        id="dept-dialog-title"
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
        <DepartmentForm
          formId={FORM_ID}
          defaultValues={defaultValues}
          onSubmit={onSubmit}
          isSubmitting={isSubmitting}
          serverErrors={serverErrors}
        />
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} disabled={isSubmitting} aria-label="Cancel">
          Cancel
        </Button>
        <Button
          type="submit"
          form={FORM_ID}
          variant="contained"
          disabled={isSubmitting}
          startIcon={isSubmitting ? <CircularProgress size={16} color="inherit" /> : null}
          aria-label={isEdit ? 'Save changes' : 'Create department'}
        >
          {isSubmitting ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Department'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
