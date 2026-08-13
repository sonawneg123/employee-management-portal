/**
 * @fileoverview DeleteDepartmentDialog — confirmation dialog for department deletion.
 *
 * Warns that deleting a department may affect employees assigned to it.
 */

import React from 'react';
import {
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Typography,
} from '@mui/material';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';

/**
 * @typedef {Object} DeleteDepartmentDialogProps
 * @property {boolean}          open
 * @property {import('@/services/departmentApi').DepartmentResponse | null} department
 * @property {boolean}          isDeleting
 * @property {() => void}       onConfirm
 * @property {() => void}       onCancel
 */

/**
 * Confirmation dialog for department deletion.
 *
 * @param {DeleteDepartmentDialogProps} props
 * @returns {JSX.Element}
 */
export default function DeleteDepartmentDialog({
  open,
  department,
  isDeleting,
  onConfirm,
  onCancel,
}) {
  const name = department?.name ?? 'this department';

  return (
    <Dialog
      open={open}
      onClose={isDeleting ? undefined : onCancel}
      maxWidth="xs"
      fullWidth
      aria-labelledby="delete-dept-title"
      aria-describedby="delete-dept-desc"
    >
      <DialogTitle id="delete-dept-title" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <WarningAmberIcon color="error" />
        Delete Department
      </DialogTitle>

      <DialogContent>
        <DialogContentText id="delete-dept-desc">
          Are you sure you want to delete{' '}
          <Typography component="span" fontWeight={700} color="text.primary">
            {name}
          </Typography>
          {department?.code && (
            <Typography component="span" variant="body2" color="text.secondary">
              {' '}
              ({department.code})
            </Typography>
          )}
          ? This may affect employees assigned to this department. This action{' '}
          <Typography component="span" fontWeight={700} color="error">
            cannot be undone
          </Typography>
          .
        </DialogContentText>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onCancel} disabled={isDeleting} aria-label="Cancel">
          Cancel
        </Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={isDeleting}
          startIcon={isDeleting ? <CircularProgress size={16} color="inherit" /> : null}
          aria-label={`Confirm delete ${name}`}
        >
          {isDeleting ? 'Deleting…' : 'Delete'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
