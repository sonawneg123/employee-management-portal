/**
 * @fileoverview DeleteEmployeeDialog — confirmation dialog for employee deletion.
 *
 * Shows the employee's name and warns that the action is irreversible.
 * Displays a loading spinner on the confirm button while the API call is pending.
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
import { formatFullName } from '@/utils/employeeFormatters';

/**
 * @typedef {Object} DeleteEmployeeDialogProps
 * @property {boolean}          open        - Controls dialog visibility.
 * @property {import('@/services/employeeApi').EmployeeResponse | null} employee
 *   - The employee to delete (used to display their name).
 * @property {boolean}          isDeleting  - Shows spinner while deleting.
 * @property {() => void}       onConfirm   - Called when the user confirms deletion.
 * @property {() => void}       onCancel    - Called when the user cancels.
 */

/**
 * Irreversible-action confirmation dialog for employee deletion.
 *
 * @param {DeleteEmployeeDialogProps} props
 * @returns {JSX.Element}
 */
export default function DeleteEmployeeDialog({
  open,
  employee,
  isDeleting,
  onConfirm,
  onCancel,
}) {
  const fullName = employee
    ? formatFullName(employee.firstName, employee.lastName)
    : 'this employee';

  return (
    <Dialog
      open={open}
      onClose={isDeleting ? undefined : onCancel}
      maxWidth="xs"
      fullWidth
      aria-labelledby="delete-employee-title"
      aria-describedby="delete-employee-description"
    >
      <DialogTitle id="delete-employee-title" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <WarningAmberIcon color="error" />
        Delete Employee
      </DialogTitle>

      <DialogContent>
        <DialogContentText id="delete-employee-description">
          Are you sure you want to delete{' '}
          <Typography component="span" fontWeight={700} color="text.primary">
            {fullName}
          </Typography>
          {employee?.employeeCode && (
            <>
              {' '}
              <Typography component="span" color="text.secondary" variant="body2">
                ({employee.employeeCode})
              </Typography>
            </>
          )}
          ? This action{' '}
          <Typography component="span" fontWeight={700} color="error">
            cannot be undone
          </Typography>
          .
        </DialogContentText>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button
          onClick={onCancel}
          disabled={isDeleting}
          aria-label="Cancel deletion"
        >
          Cancel
        </Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={isDeleting}
          startIcon={isDeleting ? <CircularProgress size={16} color="inherit" /> : null}
          aria-label={`Confirm deletion of ${fullName}`}
        >
          {isDeleting ? 'Deleting…' : 'Delete'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
